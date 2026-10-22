package com.raota.agent.application.mobile;

import com.raota.agent.application.mobile.result.MobileCurationResult;
import com.raota.agent.application.recommendation.DailyCurationService;
import com.raota.agent.domain.recommendation.model.DailyCuration;
import com.raota.mobile.shop.application.facade.MobileShopAgentFacade;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매일 06:00 날씨로 고른 라멘 종류(기존 일일 추천)에 그 종류의 v2 인기 1위 매장을 붙인다. 일일 추천은 읽기만 한다.
 *
 * <p>
 * 일일 추천의 종류 이름(예: {@code 쇼유 라멘})은 앱 종류 값으로 바꿔 매장의 라멘 종류로 찾는다. 앱 값에 없는 종류(차슈멘, 토마토 등)는 매장
 * 태그로 찾는다. 맞는 매장이 없으면 큐레이션이 없다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MobileCurationService {

    private static final Map<String, String> APP_RAMEN_TYPES = Map.ofEntries(Map.entry("쇼유", "쇼유"),
            Map.entry("쇼유파이탄", "쇼유"), Map.entry("시오", "시오"), Map.entry("돈코츠", "돈코츠"), Map.entry("미소", "미소"),
            Map.entry("이에케", "이에케"), Map.entry("토리파이탄", "토리파이탄"), Map.entry("니보시", "니보시"), Map.entry("지로", "지로"),
            Map.entry("츠케멘", "츠케멘"), Map.entry("탄탄멘", "탄탄멘"), Map.entry("마제소바", "마제소바"), Map.entry("아부라소바", "아부라소바"));

    private final DailyCurationService curations;

    private final MobileShopAgentFacade shops;

    /** 일일 추천의 종류는 지연 로딩이라 같은 읽기 트랜잭션 안에서 읽는다. */
    @Transactional(readOnly = true)
    public Optional<MobileCurationResult> today(Long viewerId) {
        Optional<DailyCuration> latest = curations.getLatestCuration();
        if (latest.isEmpty() || latest.get().getRamenType() == null) {
            return Optional.empty();
        }
        DailyCuration curation = latest.get();
        String name = normalized(curation.getRamenType().getName());
        String appType = APP_RAMEN_TYPES.get(name);
        String tagKeyword = null;
        if (appType == null) {
            tagKeyword = name;
        }
        return shops.mostPopular(viewerId, appType, tagKeyword)
            .map(shop -> new MobileCurationResult(shop, curation.getTitle(), curation.getReason()));
    }

    /** 공백과 끝의 "라멘"을 뺀다. 예: {@code 돈코츠 라멘 } → {@code 돈코츠}, {@code 토마토라멘} → {@code 토마토}. */
    static String normalized(String ramenTypeName) {
        String compact = ramenTypeName.replace(" ", "");
        if (compact.endsWith("라멘") && compact.length() > 2) {
            return compact.substring(0, compact.length() - 2);
        }
        return compact;
    }

}
