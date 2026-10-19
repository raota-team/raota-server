package com.raota.mobile.account.infrastructure.persistence;

import com.raota.mobile.account.application.port.MobileMemberCopyPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * web 모듈을 쓰지 않고 v1 테이블을 직접 읽는다. 시각은 {@link LocalDateTime}으로 주고받아 드라이버·JVM 시간대 변환 없이 저장된 값
 * 그대로 다룬다.
 */
@Repository
@RequiredArgsConstructor
public class MobileMemberJdbcCopy implements MobileMemberCopyPort {

    private final JdbcTemplate jdbc;

    @Override
    public List<LegacyMember> legacyMembers() {
        return jdbc.query("""
                SELECT id, nickname, email, image_url, bio, is_registration_completed, created_at, updated_at,
                    deleted_at
                FROM tb_member_profile ORDER BY id
                """,
                (rs, index) -> new LegacyMember(rs.getLong("id"), rs.getString("nickname"), rs.getString("email"),
                        rs.getString("image_url"), rs.getString("bio"), rs.getBoolean("is_registration_completed"),
                        time(rs, "created_at"), time(rs, "updated_at"), time(rs, "deleted_at")));
    }

    @Override
    public List<LegacySocialAccount> legacySocialAccounts() {
        return jdbc.query("SELECT id, member_id, provider, provider_id, email FROM tb_social_account ORDER BY id",
                (rs, index) -> new LegacySocialAccount(rs.getLong("id"), rs.getLong("member_id"),
                        rs.getString("provider"), rs.getString("provider_id"), rs.getString("email")));
    }

    @Override
    public Map<Long, LocalDateTime> userCreatedAts() {
        Map<Long, LocalDateTime> result = new HashMap<>();
        jdbc.query("SELECT id, created_at FROM tb_v2_user", rs -> {
            result.put(rs.getLong("id"), time(rs, "created_at"));
        });
        return result;
    }

    @Override
    public List<String> takenNicknames() {
        return jdbc.queryForList("SELECT nickname_normalized FROM tb_v2_user WHERE nickname_normalized IS NOT NULL",
                String.class);
    }

    @Override
    public List<OAuthAccountKey> oauthAccounts() {
        return jdbc.query("SELECT user_id, provider, provider_subject FROM tb_v2_user_oauth_account",
                (rs, index) -> new OAuthAccountKey(rs.getLong("user_id"), rs.getString("provider"),
                        rs.getString("provider_subject")));
    }

    @Override
    public void insertUser(NewUser user) {
        String nickname = null;
        String normalized = null;
        if (user.nickname() != null) {
            nickname = user.nickname().display();
            normalized = user.nickname().normalized();
        }
        jdbc.update("""
                INSERT INTO tb_v2_user (id, email, nickname, nickname_normalized, avatar_url, bio, role, status,
                    log_count, onboarding_completed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'USER', ?, 0, ?, ?, ?)
                """, user.id(), user.email(), nickname, normalized, user.avatarUrl(), user.bio(), user.status().name(),
                user.onboardingCompletedAt(), user.createdAt(), user.updatedAt());
    }

    @Override
    public void insertOAuthAccount(NewOAuthAccount account) {
        jdbc.update("""
                INSERT INTO tb_v2_user_oauth_account (user_id, provider, provider_subject, provider_email, linked_at,
                    last_login_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, account.userId(), account.provider().name(), account.subject(), account.email(),
                account.linkedAt(), account.linkedAt());
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

}
