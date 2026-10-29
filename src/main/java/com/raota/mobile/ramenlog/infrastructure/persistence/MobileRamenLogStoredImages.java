package com.raota.mobile.ramenlog.infrastructure.persistence;

import com.raota.global.file.cleanup.StoredImageReferences;
import com.raota.mobile.common.upload.MobileImageFolder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 기록 사진 접두어의 참조 확인. 지운 기록도 사진 행이 남아 있는 동안은 참조로 본다(회원 정리 때 행과 함께 고아가 된다). */
@Repository
@RequiredArgsConstructor
public class MobileRamenLogStoredImages implements StoredImageReferences {

    private static final int BATCH_SIZE = 500;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public String prefix() {
        return MobileImageFolder.RAMEN_LOG.prefix();
    }

    @Override
    public Set<String> referencedUrls(Set<String> urls) {
        List<String> urlList = List.copyOf(urls);
        Set<String> referenced = new HashSet<>();
        for (int start = 0; start < urlList.size(); start += BATCH_SIZE) {
            List<String> batch = urlList.subList(start, Math.min(start + BATCH_SIZE, urlList.size()));
            referenced.addAll(namedParameterJdbcTemplate.queryForList(
                    "SELECT DISTINCT url FROM tb_v2_ramen_log_image WHERE url IN (:urls)",
                    Map.of("urls", batch),
                    String.class));
        }
        return referenced;
    }
}
