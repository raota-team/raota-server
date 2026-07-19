package com.raota.integration.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberRolePersistenceTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void role을_생략한_회원_row는_USER로_저장된다() {
        jdbcTemplate.update("INSERT INTO tb_member_profile (nickname) VALUES (?)", "기본 역할 회원");

        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM tb_member_profile WHERE nickname = ?",
                String.class,
                "기본 역할 회원"
        );

        assertThat(role).isEqualTo("USER");
    }

    @Test
    void USER와_ADMIN_외의_역할은_DB에_저장할_수_없다() {
        jdbcTemplate.update("INSERT INTO tb_member_profile (nickname) VALUES (?)", "잘못된 역할 회원");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE tb_member_profile SET role = ? WHERE nickname = ?",
                "OWNER",
                "잘못된 역할 회원"
        )).isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_member_profile_role");
    }
}
