package com.raota.mobile.account.infrastructure.auth;

import com.raota.global.security.AccessLevel;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.common.MobileAuthorities;
import com.raota.mobile.common.MobilePrincipal;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record MobileAuthenticatedUser(
        Long userId, MobileUserStatus status, Collection<? extends GrantedAuthority> authorities)
        implements MobilePrincipal {

    public static MobileAuthenticatedUser of(MobileUser user) {
        String statusAuthority = MobileAuthorities.ONBOARDING;
        if (user.getStatus() == MobileUserStatus.ACTIVE) {
            statusAuthority = AccessLevel.ACTIVE_MEMBER_AUTHORITY;
        }
        SimpleGrantedAuthority membership = new SimpleGrantedAuthority(statusAuthority);
        if (user.getRole() == MobileUserRole.ADMIN) {
            return new MobileAuthenticatedUser(
                    user.getId(),
                    user.getStatus(),
                    List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            membership));
        }
        return new MobileAuthenticatedUser(
                user.getId(), user.getStatus(), List.of(new SimpleGrantedAuthority("ROLE_USER"), membership));
    }
}
