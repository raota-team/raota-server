package com.raota.account.domain.auth.repository;

import com.raota.account.domain.auth.model.AuthProvider;
import com.raota.account.domain.auth.model.SocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<SocialAccount> findByMemberId(Long memberId);

    List<SocialAccount> findAllByMemberIdIn(List<Long> memberIds);

    List<SocialAccount> findAllByMemberIdOrderByProviderAsc(Long memberId);

    void deleteByMemberId(Long memberId);
}
