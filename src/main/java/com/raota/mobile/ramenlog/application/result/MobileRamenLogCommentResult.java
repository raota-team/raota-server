package com.raota.mobile.ramenlog.application.result;

import java.time.Instant;

/** 앱의 `RamenLogComment`와 같은 형태의 댓글이다. */
public record MobileRamenLogCommentResult(String id, String logId, MobileRamenLogDetail.Author author, String content,
        Instant createdAt, boolean isMine) {
}
