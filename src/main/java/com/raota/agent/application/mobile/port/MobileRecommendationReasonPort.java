package com.raota.agent.application.mobile.port;

import java.util.Map;

/** 앱 AI 큐레이터가 고른 매장마다 왜 이 조건에 맞는지 한 문장을 쓰는 LLM이다. */
public interface MobileRecommendationReasonPort {

    /**
     * {@code conditions}는 회원이 고른 조건, {@code shopContext}는 추천된 매장 정보다. 매장 ID 문자열마다 추천 이유 한
     * 문장을 돌려준다. 모델이 빠뜨린 매장은 결과에 없다.
     */
    Map<String, String> reasons(String conditions, String shopContext);

}
