package com.raota.mobile.account.infrastructure.persistence;

import com.raota.global.file.cleanup.StoredImageReferences;
import com.raota.mobile.common.upload.MobileImageFolder;
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
        return MobileImageFolder.PROFILE.prefix();
    }

    @Override
    public Set<String> referencedUrls(Set<String> urls) {
        List<String> urlList = List.copyOf(urls);
        Set<String> referenced = new HashSet<>();
        for (int start = 0; start < urlList.size(); start += BATCH_SIZE) {
            List<String> batch = urlList.subList(start, Math.min(start + BATCH_SIZE, urlList.size()));
            referenced.addAll(namedJdbc.queryForList(
                    "SELECT DISTINCT avatar_url FROM tb_v2_user WHERE avatar_url IN (:urls)",
                    Map.of("urls", batch),
                    String.class));
        }
        return referenced;
    }
}
