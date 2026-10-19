package com.raota.global.file;

/** 웹(v1) 업로드 티켓 발급 한도를 넘었다. 429로 응답한다. */
public class UploadTicketLimitExceededException extends RuntimeException {

    public UploadTicketLimitExceededException() {
        super("사진은 1시간에 " + UploadTicketRateLimiter.FILES_PER_HOUR + "장까지 올릴 수 있습니다. 잠시 후 다시 시도해 주세요.");
    }

}
