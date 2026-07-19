package com.raota.unit.domain.member.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.model.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberProfileTest {

    @Test
    @DisplayName("신규 회원의 기본 역할은 USER다")
    void defaultRoleIsUser() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .build();

        assertThat(memberProfile.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("대표 이메일이 비어 있으면 소셜 이메일로 채운다")
    void fillEmailIfEmpty_FillsWhenEmpty() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .build();

        memberProfile.fillEmailIfEmpty("social@example.com");

        assertThat(memberProfile.getEmail()).isEqualTo("social@example.com");
    }

    @Test
    @DisplayName("대표 이메일이 있으면 소셜 이메일로 덮어쓰지 않는다")
    void fillEmailIfEmpty_KeepsExistingEmail() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .email("member@example.com")
                .build();

        memberProfile.fillEmailIfEmpty("social@example.com");

        assertThat(memberProfile.getEmail()).isEqualTo("member@example.com");
    }

    @Test
    @DisplayName("소셜 이메일이 없으면 대표 이메일을 유지한다")
    void fillEmailIfEmpty_IgnoresMissingEmail() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .email("member@example.com")
                .build();

        memberProfile.fillEmailIfEmpty(" ");

        assertThat(memberProfile.getEmail()).isEqualTo("member@example.com");
    }

    @Test
    @DisplayName("대표 이메일을 직접 수정한다")
    void updateEmail() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .email("old@example.com")
                .build();

        memberProfile.updateEmail(" new@example.com ");

        assertThat(memberProfile.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("대표 이메일은 빈 값으로 수정할 수 없다")
    void updateEmail_RejectsBlank() {
        MemberProfile memberProfile = MemberProfile.builder()
                .nickname("테스트")
                .email("old@example.com")
                .build();

        assertThatThrownBy(() -> memberProfile.updateEmail(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 null 또는 빈 값일 수 없습니다.");
    }
}
