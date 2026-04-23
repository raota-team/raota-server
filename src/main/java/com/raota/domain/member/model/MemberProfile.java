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

    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRegistrationCompleted = false;

    @Embedded
    @Builder.Default
    private MemberActivityStats memberActivityStats = MemberActivityStats.init();

    public void updateProfile(String nickname, String imageUrl, String backgroundImageUrl, String bio) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 null 또는 빈 값일 수 없습니다.");
        }

        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.bio = bio;
        this.isRegistrationCompleted = true; // 프로필 수정 시 가입 완료로 간주
    }

    public void completeRegistration() {
        this.isRegistrationCompleted = true;
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

    public void increaseBookmarkCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.increaseBookmark();
    }

    public void decreaseBookmarkCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.decreaseBookmark();
    }

    public void increasePhotoCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.increasePhoto();
    }

    public void decreasePhotoCount() {
        if (this.memberActivityStats == null) this.memberActivityStats = MemberActivityStats.init();
        this.memberActivityStats = this.memberActivityStats.decreasePhoto();
    }

    // Builder용 커스텀 메서드
    public static class MemberProfileBuilder {
        private MemberActivityStats memberActivityStats;

        public MemberProfileBuilder stats(MemberActivityStats stats) {
            this.memberActivityStats = stats;
            return this;
        }
    }
}
