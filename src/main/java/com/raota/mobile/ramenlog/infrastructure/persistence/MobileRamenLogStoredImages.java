package com.raota.mobile.ramenlog.infrastructure.persistence;

import com.raota.global.file.cleanup.StoredImageReferences;
import java.util.Collection;
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

    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public String prefix() {
        return "v2/ramen-logs/";
    }

    @Override
    public Set<String> referencedUrls(Collection<String> urls) {
        List<String> distinct = List.copyOf(new HashSet<>(urls));
        Set<String> referenced = new HashSet<>();
        for (int start = 0; start < distinct.size(); start += BATCH_SIZE) {
            List<String> batch = distinct.subList(start, Math.min(start + BATCH_SIZE, distinct.size()));
            referenced.addAll(namedJdbc.queryForList(
                    "SELECT DISTINCT url FROM tb_v2_ramen_log_image WHERE url IN (:urls)",
                    Map.of("urls", batch),
                    String.class));
        }
        return referenced;
    }
}
