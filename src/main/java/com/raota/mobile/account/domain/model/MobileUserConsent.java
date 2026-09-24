package com.raota.mobile.account.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 법적 문서 버전별 동의 또는 철회 결정을 이력으로 보관한다.
 */
@Entity
@Table(name = "tb_v2_user_consent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MobileUserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 20)
    private ConsentType consentType;

    @Column(name = "document_version", nullable = false, length = 32)
    private String documentVersion;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    private MobileUserConsent(Long userId, ConsentType type, String documentVersion, boolean granted,
            Instant decidedAt) {
        this.userId = userId;
        this.consentType = type;
        this.documentVersion = documentVersion;
        this.granted = granted;
        this.decidedAt = decidedAt;
    }

    public static MobileUserConsent decide(Long userId, ConsentType type, String documentVersion, boolean granted,
            Instant decidedAt) {
        return new MobileUserConsent(userId, type, documentVersion, granted, decidedAt);
    }

}
