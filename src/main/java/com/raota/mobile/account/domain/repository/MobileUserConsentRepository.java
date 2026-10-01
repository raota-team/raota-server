package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserConsentRepository extends JpaRepository<MobileUserConsent, Long> {

    @Modifying
    @Query("delete from MobileUserConsent consent where consent.userId = :userId")
    void deleteByUserId(Long userId);

}
