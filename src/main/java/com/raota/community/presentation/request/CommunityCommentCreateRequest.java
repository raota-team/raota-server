package com.raota.community.presentation.request;

import com.raota.community.application.command.CreateCommentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentCreateRequest {
    @Schema(description = "댓글 내용")
    private String content;

    @Schema(description = "부모 댓글 ID(답글일 때만)", nullable = true)
    private Long parentCommentId;

    public CreateCommentCommand toCommand(Long postId, Long authorId) {
        return new CreateCommentCommand(postId, authorId, parentCommentId, content);
    }
}
