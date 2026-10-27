package com.raota.global.file.cleanup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 고아 사진 정리 설정. {@code app.file.orphan-cleanup.enabled=false}면 정리 작업을 등록하지 않는다.
 *
 * @param retention 마지막 수정 뒤 이 기간이 지난 객체만 정리한다
 * @param maxDeletes 한 번 실행에서 시도하는 최대 삭제 수
 */
@ConfigurationProperties("app.file.orphan-cleanup")
@Validated
public record OrphanImageCleanupProperties(
        @NotNull @DurationMin(days = 1) @DefaultValue("P7D") Duration retention,
        @Positive @DefaultValue("500") int maxDeletes) {}
