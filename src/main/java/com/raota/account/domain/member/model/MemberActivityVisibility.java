package com.raota.account.domain.member.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberActivityVisibility {

    @Builder.Default
    @Column(name = "logs_public", nullable = false)
    private boolean logsPublic = true;

    @Builder.Default
    @Column(name = "visits_public", nullable = false)
    private boolean visitsPublic = true;

    @Builder.Default
    @Column(name = "posts_public", nullable = false)
    private boolean postsPublic = true;

    @Builder.Default
    @Column(name = "comments_public", nullable = false)
    private boolean commentsPublic = true;

    public static MemberActivityVisibility allPublic() {
        return MemberActivityVisibility.builder().build();
    }

    public void update(boolean logs, boolean visits, boolean posts, boolean comments) {
        this.logsPublic = logs;
        this.visitsPublic = visits;
        this.postsPublic = posts;
        this.commentsPublic = comments;
    }
}
