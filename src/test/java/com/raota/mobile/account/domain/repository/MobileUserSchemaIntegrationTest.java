package com.raota.mobile.account.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.support.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MobileUserSchemaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MobileUserRepository userRepository;

    @Autowired
    private MobileUserOAuthAccountRepository oauthAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 온보딩_회원은_생성_수정_시각과_초기_상태가_저장된다() {
        Long userId = userRepository.saveAndFlush(MobileUser.onboarding("onboarding@example.com")).getId();
        entityManager.clear();

        MobileUser saved = userRepository.findById(userId).orElseThrow();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(MobileUserRole.USER);
        assertThat(saved.getStatus()).isEqualTo(MobileUserStatus.ONBOARDING);
        assertThat(saved.getLogCount()).isZero();
    }

    @Test
    void 동일한_제공자와_제공자_사용자_ID로_계정을_중복_연결할_수_없다() {
        Long firstUserId = userRepository.saveAndFlush(MobileUser.onboarding("first@example.com")).getId();
        Long secondUserId = userRepository.saveAndFlush(MobileUser.onboarding("second@example.com")).getId();
        Instant now = Instant.parse("2026-09-25T10:00:00Z");

        oauthAccountRepository
            .saveAndFlush(MobileUserOAuthAccount.link(firstUserId, OAuthProvider.KAKAO, "same-subject", null, now));

        assertThatThrownBy(() -> oauthAccountRepository
            .saveAndFlush(MobileUserOAuthAccount.link(secondUserId, OAuthProvider.KAKAO, "same-subject", null, now)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 한_회원은_같은_제공자를_두_번_연결할_수_없다() {
        Long userId = userRepository.saveAndFlush(MobileUser.onboarding("linked@example.com")).getId();
        Instant now = Instant.parse("2026-09-25T10:00:00Z");

        oauthAccountRepository
            .saveAndFlush(MobileUserOAuthAccount.link(userId, OAuthProvider.GOOGLE, "first-subject", null, now));

        assertThatThrownBy(() -> oauthAccountRepository
            .saveAndFlush(MobileUserOAuthAccount.link(userId, OAuthProvider.GOOGLE, "second-subject", null, now)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 빈_정규화_닉네임은_중복을_허용하지만_같은_닉네임은_거부한다() {
        userRepository.saveAndFlush(MobileUser.onboarding("one@example.com"));
        userRepository.saveAndFlush(MobileUser.onboarding("two@example.com"));

        insertUserWithNickname("라멘");

        assertThatThrownBy(() -> insertUserWithNickname("라멘")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 정의되지_않은_회원_상태는_저장할_수_없다() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tb_v2_user (role, status, log_count, created_at, updated_at)
                VALUES ('USER', 'INVALID', 0, NOW(6), NOW(6))
                """)).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_v2_user_status");
    }

    private void insertUserWithNickname(String nicknameNormalized) {
        jdbcTemplate.update("""
                INSERT INTO tb_v2_user (nickname, nickname_normalized, role, status, log_count, created_at, updated_at)
                VALUES (?, ?, 'USER', 'ACTIVE', 0, NOW(6), NOW(6))
                """, nicknameNormalized, nicknameNormalized);
    }

}
