package com.raota.mobile.account.infrastructure.auth;

import com.raota.mobile.account.application.port.AuthenticatedMobileUser;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserRole;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record MobileAuthenticatedUser(Long userId, MobileUserStatus status,
        Collection<? extends GrantedAuthority> authorities) implements AuthenticatedMobileUser {

    public static MobileAuthenticatedUser of(MobileUser user) {
        List<SimpleGrantedAuthority> authorities = user.getRole() == MobileUserRole.ADMIN
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new MobileAuthenticatedUser(user.getId(), user.getStatus(), authorities);
    }

}
