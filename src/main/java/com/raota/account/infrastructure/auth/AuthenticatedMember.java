package com.raota.account.infrastructure.auth;

import com.raota.account.domain.member.model.MemberRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedMember(Long memberId, Collection<? extends GrantedAuthority> authorities) {

    public static AuthenticatedMember of(Long memberId, MemberRole role) {
        List<SimpleGrantedAuthority> authorities = switch (role) {
            case USER -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
            case ADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        };
        return new AuthenticatedMember(memberId, authorities);
    }
}
