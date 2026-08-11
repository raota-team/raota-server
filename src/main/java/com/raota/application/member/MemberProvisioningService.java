package com.raota.application.member;

import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.model.MemberRole;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.account.infrastructure.auth.WithdrawnMemberException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberProvisioningService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberProfile createOAuthMember(String nickname, String profileImageUrl) {
        return memberRepository.save(MemberProfile.builder()
                .nickname(nickname)
                .imageUrl(profileImageUrl)
                .backgroundImageUrl(null)
                .stats(MemberActivityStats.init())
                .build());
    }

    @Transactional(readOnly = true)
    public Optional<MemberProfile> findById(Long memberId) {
        return memberRepository.findById(memberId);
    }

    @Transactional(readOnly = true)
    public MemberProfile getRequired(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 유저 정보 입니다."));
    }

    @Transactional(readOnly = true)
    public MemberProfile getActiveRequired(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new WithdrawnMemberException(MemberLifecycleService.WITHDRAWN_MEMBER_MESSAGE));
    }

    @Transactional(readOnly = true)
    public Optional<MemberRole> findActiveMemberRole(Long memberId) {
        return memberRepository.findActiveMemberRole(memberId);
    }

    @Transactional
    public void deleteById(Long memberId) {
        memberRepository.deleteById(memberId);
    }
}
