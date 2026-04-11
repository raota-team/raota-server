package com.raota.domain.member.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MemberProfile {

    @Builder
    public MemberProfile(Long id, String imageUrl, String backgroundImageUrl, String nickname, MemberActivityStats stats) {
        verifyNicknameBlank(nickname);
        this.id = id;
        this.imageUrl = imageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.nickname = nickname;
        this.memberActivityStats = stats;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    private String imageUrl;

    private String backgroundImageUrl;

    @Embedded
    private MemberActivityStats memberActivityStats;

    public void updateProfile(String nickname, String imageUrl, String backgroundImageUrl) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 null 또는 빈 값일 수 없습니다.");
        }

        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public void increasePostCount() {
        this.memberActivityStats = this.memberActivityStats.increasePost();
    }

    public void decreasePostCount() {
        this.memberActivityStats = this.memberActivityStats.decreasePost();
    }

    public void increaseCommentCount() {
        this.memberActivityStats = this.memberActivityStats.increaseComment();
    }

    public void decreaseCommentCount() {
        this.memberActivityStats = this.memberActivityStats.decreaseComment();
    }

    private void verifyNicknameBlank(String nickname) {
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 공백일수 없습니다.");
        }
    }

}
