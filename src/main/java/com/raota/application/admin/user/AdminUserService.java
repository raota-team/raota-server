package com.raota.application.admin.user;

import com.raota.domain.auth.model.AuthProvider;
import com.raota.domain.auth.model.SocialAccount;
import com.raota.domain.auth.repository.SocialAccountRepository;
import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberActivityVisibility;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.infrastructure.file.FileUploader;
import com.raota.presentation.admin.user.response.AdminUserActivityStatsResponse;
import com.raota.presentation.admin.user.response.AdminUserActivityVisibilityResponse;
import com.raota.presentation.admin.user.response.AdminUserDetailResponse;
import com.raota.presentation.admin.user.response.AdminUserListItemResponse;
import com.raota.presentation.admin.user.response.AdminUserProfileResponse;
import com.raota.presentation.admin.user.response.AdminUserSocialAccountResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final FileUploader fileUploader;

    @Transactional(readOnly = true)
    public Page<AdminUserListItemResponse> getUsers(
            String keyword,
            Boolean registrationCompleted,
            Boolean deleted,
            AuthProvider provider,
            Boolean emailPresent,
            int page,
            int size
    ) {
        String normalizedKeyword = normalize(keyword);
        Long keywordMemberId = parseMemberId(normalizedKeyword);
        Page<MemberProfile> members = memberRepository.findAdminUsers(
                normalizedKeyword == null,
                normalizedKeyword == null ? "%" : "%" + normalizedKeyword.toLowerCase() + "%",
                keywordMemberId,
                registrationCompleted,
                deleted,
                provider == null ? null : provider.name(),
                emailPresent,
                PageRequest.of(Math.max(page, 0), normalizeSize(size))
        );
        Map<Long, List<SocialAccount>> socialAccountsByMemberId = socialAccountsByMemberId(members.getContent());

        return members.map(member -> AdminUserListItemResponse.from(
                member,
                socialAccountsByMemberId.getOrDefault(member.getId(), List.of())
        ));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long memberId) {
        MemberProfile member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 유저 정보 입니다."));
        List<AdminUserSocialAccountResponse> socialAccounts = socialAccountRepository
                .findAllByMemberIdOrderByProviderAsc(memberId)
                .stream()
                .map(AdminUserSocialAccountResponse::from)
                .toList();

        return new AdminUserDetailResponse(
                AdminUserProfileResponse.from(
                        member,
                        accessibleUrl(member.getImageUrl()),
                        accessibleUrl(member.getBackgroundImageUrl())
                ),
                socialAccounts,
                AdminUserActivityStatsResponse.from(stats(member)),
                AdminUserActivityVisibilityResponse.from(visibility(member))
        );
    }

    private Map<Long, List<SocialAccount>> socialAccountsByMemberId(List<MemberProfile> members) {
        List<Long> memberIds = members.stream()
                .map(MemberProfile::getId)
                .filter(Objects::nonNull)
                .toList();
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        return socialAccountRepository.findAllByMemberIdIn(memberIds)
                .stream()
                .sorted(Comparator.comparing(SocialAccount::getProvider))
                .collect(Collectors.groupingBy(SocialAccount::getMemberId));
    }

    private String accessibleUrl(String path) {
        return path == null || path.isBlank() ? null : fileUploader.getAccessibleUrl(path);
    }

    private MemberActivityStats stats(MemberProfile member) {
        return member.getMemberActivityStats() == null ? MemberActivityStats.init() : member.getMemberActivityStats();
    }

    private MemberActivityVisibility visibility(MemberProfile member) {
        return member.getActivityVisibility() == null ? MemberActivityVisibility.allPublic() : member.getActivityVisibility();
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Long parseMemberId(String keyword) {
        if (keyword == null || !keyword.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
