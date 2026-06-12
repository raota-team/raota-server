package com.raota.application.member;

import com.raota.application.auth.AuthAccountService;
import com.raota.domain.auth.repository.SocialAccountRepository;
import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.BookmarkRepository;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.repository.RamenProofPictureRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLifecycleService {

    public static final int REJOIN_WAIT_DAYS = 30;
    public static final String WITHDRAWN_MEMBER_MESSAGE = "탈퇴 처리된 계정입니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다.";
    public static final String WITHDRAW_COMPLETE_MESSAGE = "회원 탈퇴가 완료되었습니다. 탈퇴일로부터 30일 후 재가입할 수 있습니다.";

    private final MemberRepository memberRepository;
    private final MemberProvisioningService memberProvisioningService;
    private final AuthAccountService authAccountService;
    private final SocialAccountRepository socialAccountRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RamenProofPictureRepository ramenProofPictureRepository;

    @Transactional
    public void withdraw(Long memberId) {
        MemberProfile member = memberProvisioningService.getActiveRequired(memberId);
        member.softDelete(LocalDateTime.now());
        authAccountService.logoutByMemberId(memberId);
    }

    @Transactional
    public int purgeExpiredMembers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(REJOIN_WAIT_DAYS);
        List<MemberProfile> expiredMembers = memberRepository.findSoftDeletedMembersDueForPurge(cutoff);

        expiredMembers.forEach(member -> {
            Long memberId = member.getId();
            authAccountService.logoutByMemberId(memberId);
            socialAccountRepository.deleteByMemberId(memberId);
            bookmarkRepository.deleteAllByMemberProfileId(memberId);
            ramenProofPictureRepository.deleteAllByMemberProfileId(memberId);
            member.anonymizeRetainedProfile();
        });

        if (!expiredMembers.isEmpty()) {
            log.info("Purged {} withdrawn members", expiredMembers.size());
        }
        return expiredMembers.size();
    }
}
