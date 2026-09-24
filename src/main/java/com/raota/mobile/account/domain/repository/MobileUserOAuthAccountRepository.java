package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileUserOAuthAccountRepository extends JpaRepository<MobileUserOAuthAccount, Long> {

}
