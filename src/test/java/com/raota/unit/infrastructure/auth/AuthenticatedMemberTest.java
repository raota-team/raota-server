package com.raota.unit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.account.domain.member.model.MemberRole;
import com.raota.account.infrastructure.auth.AuthenticatedMember;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class AuthenticatedMemberTest {

    @Test
    void USER는_일반_사용자_권한만_가진다() {
        AuthenticatedMember member = AuthenticatedMember.of(1L, MemberRole.USER);

        assertThat(member.authorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void ADMIN은_일반_사용자와_관리자_권한을_모두_가진다() {
        AuthenticatedMember member = AuthenticatedMember.of(1L, MemberRole.ADMIN);

        assertThat(member.authorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }
}
