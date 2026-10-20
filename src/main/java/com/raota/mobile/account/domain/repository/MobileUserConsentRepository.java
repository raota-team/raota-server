package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUserConsent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserConsentRepository extends JpaRepository<MobileUserConsent, Long> {

    @Modifying
    @Query("delete from MobileUserConsent consent where consent.userId = :userId")
    void deleteByUserId(Long userId);

    /** 유형별로 동의한 가장 높은 문서 버전이다. 각 행은 {@code [ConsentType, documentVersion]}이다. */
    @Query("""
            select consent.consentType, max(consent.documentVersion) from MobileUserConsent consent
            where consent.userId = :userId and consent.granted = true
            group by consent.consentType
            """)
    List<Object[]> findLatestGrantedVersions(Long userId);

}
