package com.raota.mobile.account.infrastructure.persistence;

import com.raota.global.file.cleanup.StoredImageReferences;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 프로필 사진 접두어의 참조 확인. 탈퇴 유예 중인 회원처럼 행에 주소가 남아 있으면 참조로 본다. */
@Repository
@RequiredArgsConstructor
public class MobileProfileStoredImages implements StoredImageReferences {

    private static final int BATCH_SIZE = 500;

    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public String prefix() {
        return "v2/profiles/";
    }

    @Override
    public Set<String> referencedUrls(Collection<String> urls) {
        List<String> distinct = List.copyOf(new HashSet<>(urls));
        Set<String> referenced = new HashSet<>();
        for (int start = 0; start < distinct.size(); start += BATCH_SIZE) {
            List<String> batch = distinct.subList(start, Math.min(start + BATCH_SIZE, distinct.size()));
            referenced
                .addAll(namedJdbc.queryForList("SELECT DISTINCT avatar_url FROM tb_v2_user WHERE avatar_url IN (:urls)",
                        Map.of("urls", batch), String.class));
        }
        return referenced;
    }

}
