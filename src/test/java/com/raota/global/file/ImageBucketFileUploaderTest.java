package com.raota.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raota.global.presentation.file.response.PresignedUrlResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 운영 업로드 서명이 저장소에 크기·형식·키 조건을 거는지 확인한다(#84). 조건이 빠지면 큰 파일이나 다른 형식을 막지 못한다. */
class ImageBucketFileUploaderTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final ImageBucketFileUploader uploader = new ImageBucketFileUploader(
            new PresignedPostSigner(
                    "https://ns.compat.objectstorage.ap-chuncheon-1.oraclecloud.com",
                    "raota",
                    "ap-chuncheon-1",
                    "AKID",
                    "SECRET",
                    mapper,
                    Clock.fixed(Instant.parse("2026-10-19T05:00:00Z"), ZoneOffset.UTC)),
            null);

    @Test
    void 사진_티켓은_10MB_이하와_서명한_형식과_키만_허용하는_POST_조건을_준다() {
        PresignedUrlResponse ticket = uploader.getPresignedUrl("v2/ramen-logs", ".jpg", "image/jpeg");

        assertThat(ticket.uploadUrl())
                .isEqualTo("https://ns.compat.objectstorage.ap-chuncheon-1.oraclecloud.com/raota");
        String key = (String) ticket.uploadParams().get("key");
        assertThat(key).startsWith("v2/ramen-logs/").endsWith(".jpg");
        assertThat(ticket.imgUrl()).isEqualTo("https://images.raota.net/" + key);
        assertThat(ticket.uploadParams())
                .containsEntry("Content-Type", "image/jpeg")
                .containsEntry("x-amz-algorithm", "AWS4-HMAC-SHA256")
                .containsEntry("x-amz-credential", "AKID/20261019/ap-chuncheon-1/s3/aws4_request")
                .containsKey("x-amz-signature");

        JsonNode policy = mapper.readTree(Base64.getDecoder()
                .decode(((String) ticket.uploadParams().get("policy")).getBytes(StandardCharsets.UTF_8)));
        assertThat(policy.get("expiration").asString()).isEqualTo("2026-10-19T05:05:00Z");
        assertThat(policy.get("conditions").toString())
                .contains("{\"bucket\":\"raota\"}")
                .contains("{\"key\":\"" + key + "\"}")
                .contains("{\"Content-Type\":\"image/jpeg\"}")
                .contains("[\"content-length-range\",1," + ImageBucketFileUploader.MAX_IMAGE_BYTES + "]");
    }

    @Test
    void 형식을_주지_않으면_확장자로_정하고_이미지가_아니거나_어긋나면_거절한다() {
        assertThat(uploader.getPresignedUrl("community", "webp", null).uploadParams())
                .containsEntry("Content-Type", "image/webp");

        assertThatThrownBy(() -> uploader.getPresignedUrl("community", "html", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> uploader.getPresignedUrl("community", "png", "text/html"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
