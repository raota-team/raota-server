package com.raota.domain.member.service;

import com.raota.domain.member.model.MemberActivityStats;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
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
    public MemberProfile getRequired(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("없는 유저 정보 입니다."));
    }

    @Transactional
    public void deleteById(Long memberId) {
        memberRepository.deleteById(memberId);
    }
}
