package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.config.MobileAccountProperties;
import com.raota.mobile.account.domain.model.ConsentType;
import com.raota.mobile.account.domain.repository.MobileUserConsentRepository;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원이 현재 버전의 이용약관과 개인정보처리방침에 동의했는지 판단한다. 버전은 시행일(YYYY-MM-DD)이라 문자열 비교가 날짜 비교와 같다. v1에서 옮긴
 * 회원은 동의 기록이 없어 앱 첫 로그인 때 동의를 받는다.
 */
@Component
@RequiredArgsConstructor
class MobileConsentPolicy {

    private final MobileUserConsentRepository consents;

    private final MobileAccountProperties properties;

    boolean required(Long userId) {
        Map<ConsentType, String> latest = new EnumMap<>(ConsentType.class);
        for (Object[] row : consents.findLatestGrantedVersions(userId)) {
            latest.put((ConsentType) row[0], (String) row[1]);
        }
        return !covers(latest.get(ConsentType.TERMS), properties.requiredTermsVersion())
                || !covers(latest.get(ConsentType.PRIVACY), properties.requiredPrivacyVersion());
    }

    boolean isRequiredType(ConsentType type) {
        return type == ConsentType.TERMS || type == ConsentType.PRIVACY;
    }

    private static boolean covers(String granted, String required) {
        return granted != null && granted.compareTo(required) >= 0;
    }
}
