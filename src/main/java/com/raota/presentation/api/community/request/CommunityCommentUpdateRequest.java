package com.raota.presentation.api.community.request;

import com.raota.application.community.command.UpdateCommentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CommunityCommentUpdateRequest {
    @Schema(description = "댓글 내용")
    private String content;

    public UpdateCommentCommand toCommand(Long commentId, Long authorId) {
        return new UpdateCommentCommand(commentId, authorId, content);
    }
}
