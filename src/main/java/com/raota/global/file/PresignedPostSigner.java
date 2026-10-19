package com.raota.global.file;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.ObjectMapper;

/**
 * S3 호환 저장소(OCI Object Storage)에 브라우저·앱이 직접 올리는 POST 업로드 서명(SigV4 POST policy)을 만든다.
 *
 * <p>
 * 미리 서명한 PUT URL은 올리는 파일 크기를 막지 못한다. POST policy의 {@code content-length-range}와
 * {@code Content-Type} 조건은 저장소가 직접 검사하므로, 서명 뒤 다른 크기나 형식의 파일을 올리면 저장소가 거절한다.
 * </p>
 */
public class PresignedPostSigner {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter SCOPE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter EXPIRATION = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneOffset.UTC);

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private final String bucketUrl;

    private final String bucket;

    private final String region;

    private final String accessKey;

    private final String secretKey;

    private final ObjectMapper mapper;

    private final Clock clock;

    public PresignedPostSigner(String endpoint, String bucket, String region, String accessKey, String secretKey,
            ObjectMapper mapper, Clock clock) {
        this.bucketUrl = endpoint + "/" + bucket;
        this.bucket = bucket;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** {@code fields}는 순서대로 form 필드로 보내고 파일은 마지막 {@code file} 필드로 보낸다. */
    public PresignedPost sign(String objectKey, String contentType, long maxBytes, Duration validity) {
        Instant now = clock.instant();
        String scopeDate = SCOPE_DATE.format(now);
        String credential = accessKey + "/" + scopeDate + "/" + region + "/s3/aws4_request";
        String amzDate = AMZ_DATE.format(now);

        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("expiration", EXPIRATION.format(now.plus(validity)));
        policy.put("conditions",
                List.of(Map.of("bucket", bucket), Map.of("key", objectKey), Map.of("Content-Type", contentType),
                        List.of("content-length-range", 1, maxBytes), Map.of("x-amz-algorithm", ALGORITHM),
                        Map.of("x-amz-credential", credential), Map.of("x-amz-date", amzDate)));
        String encodedPolicy = Base64.getEncoder()
            .encodeToString(mapper.writeValueAsString(policy).getBytes(StandardCharsets.UTF_8));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", objectKey);
        fields.put("Content-Type", contentType);
        fields.put("x-amz-algorithm", ALGORITHM);
        fields.put("x-amz-credential", credential);
        fields.put("x-amz-date", amzDate);
        fields.put("policy", encodedPolicy);
        fields.put("x-amz-signature", HexFormat.of().formatHex(hmac(signingKey(scopeDate), encodedPolicy)));
        return new PresignedPost(bucketUrl, fields);
    }

    private byte[] signingKey(String scopeDate) {
        byte[] date = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), scopeDate);
        byte[] regionKey = hmac(date, region);
        byte[] service = hmac(regionKey, "s3");
        return hmac(service, "aws4_request");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("업로드 서명을 만들 수 없습니다.", exception);
        }
    }

    public record PresignedPost(String url, Map<String, String> fields) {
    }

}
