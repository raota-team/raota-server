package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.result.MobileAdminUserDetailResult;
import com.raota.mobile.account.application.result.MobileAdminUserDetailResult.OAuthAccount;
import com.raota.mobile.account.application.result.MobileAdminUserResult;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserOAuthAccount;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.account.domain.repository.MobileUserOAuthAccountRepository;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.common.page.OffsetPage;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 웹의 앱 회원 조회다. 조회만 하므로 회원 상태는 바꾸지 않는다. 로그인 수단은 페이지 단위로 한 번에 읽어 회원마다 따로 조회하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MobileUserAdminService {

    private static final int MAX_USER_ID_DIGITS = 18;

    private final MobileUserRepository userRepository;

    private final MobileUserOAuthAccountRepository oauthAccountRepository;

    /** {@code keyword}는 닉네임·이메일 부분 일치이고, 숫자만 있으면 회원 ID 일치도 본다. 최근 가입한 회원부터 보여 준다. */
    @Transactional(readOnly = true)
    public OffsetPage<MobileAdminUserResult> list(String keyword, MobileUserStatus status, int page, int size) {
        String normalized = blankToNull(keyword);
        Page<MobileUser> found = userRepository.findForAdmin(
                normalized,
                toUserId(normalized),
                status,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        Map<Long, List<MobileUserOAuthAccount>> accounts = accountsByUser(
                found.getContent().stream().map(MobileUser::getId).toList());
        return OffsetPage.of(found, user -> toRow(user, accounts.getOrDefault(user.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public MobileAdminUserDetailResult detail(Long userId) {
        MobileUser user = userRepository
                .findById(userId)
                .orElseThrow(() -> new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "회원을 찾을 수 없습니다."));
        List<MobileUserOAuthAccount> accounts = oauthAccountRepository.findAllByUserId(userId);
        MobileAdminUserResult row = toRow(user, accounts);
        List<OAuthAccount> oauthAccounts = accounts.stream()
                .sorted(Comparator.comparing(MobileUserOAuthAccount::getLinkedAt))
                .map(account -> new OAuthAccount(
                        account.getProvider(),
                        account.getProviderEmail(),
                        account.getLinkedAt(),
                        account.getLastLoginAt()))
                .toList();
        return new MobileAdminUserDetailResult(
                row.id(),
                row.nickname(),
                row.email(),
                row.role(),
                row.status(),
                row.providers(),
                row.logCount(),
                row.createdAt(),
                row.onboardingCompletedAt(),
                row.withdrawalRequestedAt(),
                row.purgeScheduledAt(),
                row.lastLoginAt(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getFavoriteRamenType(),
                user.getUpdatedAt(),
                oauthAccounts);
    }

    private Map<Long, List<MobileUserOAuthAccount>> accountsByUser(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return oauthAccountRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(MobileUserOAuthAccount::getUserId));
    }

    private static MobileAdminUserResult toRow(MobileUser user, List<MobileUserOAuthAccount> accounts) {
        List<OAuthProvider> providers = accounts.stream()
                .map(MobileUserOAuthAccount::getProvider)
                .distinct()
                .sorted()
                .toList();
        Instant lastLoginAt = accounts.stream()
                .map(MobileUserOAuthAccount::getLastLoginAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new MobileAdminUserResult(
                String.valueOf(user.getId()),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                providers,
                user.getLogCount(),
                user.getCreatedAt(),
                user.getOnboardingCompletedAt(),
                user.getWithdrawalRequestedAt(),
                user.getPurgeScheduledAt(),
                lastLoginAt);
    }

    /** Long 범위를 넘는 숫자는 ID가 될 수 없으므로 18자리까지만 ID로 본다. */
    private static Long toUserId(String keyword) {
        if (keyword == null
                || keyword.length() > MAX_USER_ID_DIGITS
                || !keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.valueOf(keyword);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
