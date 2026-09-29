package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileUserOAuthAccountRepository extends JpaRepository<MobileUserOAuthAccount, Long> {

    Optional<MobileUserOAuthAccount> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);

}
