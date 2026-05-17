package com.raota.infrastructure.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedMember(Long memberId, Collection<? extends GrantedAuthority> authorities) {

    public static AuthenticatedMember user(Long memberId) {
        return new AuthenticatedMember(memberId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
