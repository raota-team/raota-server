package com.raota.domain.member.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_member_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    private String imageUrl;

    private String backgroundImageUrl;

    @Embedded
    @Builder.Default
    private MemberActivityStats memberActivityStats = MemberActivityStats.init();

    public void updateProfile(String nickname, String imageUrl, String backgroundImageUrl) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 null 또는 빈 값일 수 없습니다.");
        }

        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public void increasePostCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.increasePost();
    }

    public void decreasePostCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.decreasePost();
    }

    public void increaseCommentCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.increaseComment();
    }

    public void decreaseCommentCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.decreaseComment();
    }

    // Builder용 커스텀 메서드
    public static class MemberProfileBuilder {
        private MemberActivityStats memberActivityStats; // 필드명 일치

        public MemberProfileBuilder stats(MemberActivityStats stats) {
            this.memberActivityStats = stats;
            return this;
        }
    }
}
