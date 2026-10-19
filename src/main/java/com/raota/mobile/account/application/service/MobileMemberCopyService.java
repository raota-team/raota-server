package com.raota.mobile.account.application.service;

import com.raota.mobile.account.application.port.MobileMemberCopyPort;
import com.raota.mobile.account.application.port.MobileMemberCopyPort.LegacyMember;
import com.raota.mobile.account.application.port.MobileMemberCopyPort.LegacySocialAccount;
import com.raota.mobile.account.application.port.MobileMemberCopyPort.NewOAuthAccount;
import com.raota.mobile.account.application.port.MobileMemberCopyPort.NewUser;
import com.raota.mobile.account.application.port.MobileMemberCopyPort.OAuthAccountKey;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.model.OAuthProvider;
import com.raota.mobile.common.copy.LegacyCopyExclusion;
import com.raota.mobile.common.copy.LegacyCopyReport;
import com.raota.mobile.common.copy.LegacyCopyTally;
import com.raota.mobile.common.copy.LegacyKstTime;
import com.raota.mobile.common.error.MobileException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v1 회원과 소셜 계정을 ID를 유지해 v2로 복사한다(#59). 이미 있는 행은 갱신하지 않고 건너뛰어 다시 실행해도 그 사이 새로 생긴 행만 옮긴다.
 */
@Service
@RequiredArgsConstructor
public class MobileMemberCopyService {

    private static final int EMAIL_MAX_LENGTH = 255;

    private static final int BIO_MAX_LENGTH = 500;

    private final MobileMemberCopyPort port;

    @Transactional
    public LegacyCopyReport copy() {
        LegacyCopyTally userTally = new LegacyCopyTally("tb_v2_user");
        LegacyCopyTally accountTally = new LegacyCopyTally("tb_v2_user_oauth_account");
        Map<Long, LocalDateTime> userCreatedAts = port.userCreatedAts();
        Set<String> takenNicknames = new HashSet<>(port.takenNicknames());
        Map<Long, LegacyMember> members = new HashMap<>();
        Set<Long> copiedMembers = new HashSet<>();
        for (LegacyMember member : port.legacyMembers()) {
            members.put(member.id(), member);
            LocalDateTime createdAt = LegacyKstTime.toUtc(member.createdAt());
            LocalDateTime existing = userCreatedAts.get(member.id());
            if (existing != null) {
                if (existing.equals(createdAt)) {
                    userTally.alreadyCopied();
                    copiedMembers.add(member.id());
                }
                else {
                    userTally.exclude(LegacyCopyExclusion.ID_CONFLICT);
                }
                continue;
            }
            if (member.deletedAt() != null) {
                userTally.exclude(LegacyCopyExclusion.WITHDRAWN_MEMBER);
                continue;
            }
            port.insertUser(newUser(member, createdAt, takenNicknames));
            userTally.copied();
            copiedMembers.add(member.id());
        }
        copyAccounts(members, copiedMembers, accountTally);
        return LegacyCopyReport.of(userTally, accountTally);
    }

    /** v2 회원 중 v1 회원을 복사한 것(같은 ID에 같은 생성 시각)의 ID다. */
    @Transactional(readOnly = true)
    public Set<Long> copiedMemberIds() {
        Map<Long, LocalDateTime> userCreatedAts = port.userCreatedAts();
        Set<Long> result = new HashSet<>();
        for (LegacyMember member : port.legacyMembers()) {
            LocalDateTime existing = userCreatedAts.get(member.id());
            if (existing != null && existing.equals(LegacyKstTime.toUtc(member.createdAt()))) {
                result.add(member.id());
            }
        }
        return result;
    }

    private void copyAccounts(Map<Long, LegacyMember> members, Set<Long> copiedMembers, LegacyCopyTally tally) {
        Map<String, Long> subjectOwners = new HashMap<>();
        Set<String> userProviders = new HashSet<>();
        for (OAuthAccountKey key : port.oauthAccounts()) {
            subjectOwners.put(key.provider() + ":" + key.subject(), key.userId());
            userProviders.add(key.userId() + ":" + key.provider());
        }
        for (LegacySocialAccount account : port.legacySocialAccounts()) {
            Long owner = subjectOwners.get(account.provider() + ":" + account.providerId());
            if (owner != null) {
                if (owner.equals(account.memberId())) {
                    tally.alreadyCopied();
                }
                else {
                    tally.exclude(LegacyCopyExclusion.DUPLICATE_SUBJECT);
                }
                continue;
            }
            LegacyCopyExclusion exclusion = accountExclusion(account, members.get(account.memberId()), copiedMembers);
            if (exclusion == null && !userProviders.add(account.memberId() + ":" + account.provider())) {
                exclusion = LegacyCopyExclusion.DUPLICATE_PROVIDER;
            }
            if (exclusion != null) {
                tally.exclude(exclusion);
                continue;
            }
            port.insertOAuthAccount(new NewOAuthAccount(account.memberId(), OAuthProvider.valueOf(account.provider()),
                    account.providerId(), email(account.email()),
                    LegacyKstTime.toUtc(members.get(account.memberId()).createdAt())));
            subjectOwners.put(account.provider() + ":" + account.providerId(), account.memberId());
            tally.copied();
        }
    }

    private LegacyCopyExclusion accountExclusion(LegacySocialAccount account, LegacyMember member,
            Set<Long> copiedMembers) {
        if (member == null) {
            return LegacyCopyExclusion.ORPHAN;
        }
        if (member.deletedAt() != null) {
            return LegacyCopyExclusion.WITHDRAWN_MEMBER;
        }
        if (!copiedMembers.contains(member.id())) {
            return LegacyCopyExclusion.MEMBER_NOT_COPIED;
        }
        if (!isProvider(account.provider())) {
            return LegacyCopyExclusion.UNKNOWN_PROVIDER;
        }
        return null;
    }

    private NewUser newUser(LegacyMember member, LocalDateTime createdAt, Set<String> takenNicknames) {
        Nickname nickname = nickname(member.nickname(), takenNicknames);
        MobileUserStatus status = MobileUserStatus.ONBOARDING;
        LocalDateTime onboardingCompletedAt = null;
        if (member.registrationCompleted() && nickname != null) {
            status = MobileUserStatus.ACTIVE;
            onboardingCompletedAt = createdAt;
        }
        return new NewUser(member.id(), email(member.email()), nickname, blankToNull(member.imageUrl()),
                bio(member.bio()), status, onboardingCompletedAt, createdAt, LegacyKstTime.toUtc(member.updatedAt()));
    }

    /**
     * v2 닉네임 규칙(NFKC·trim·허용 문자·2~12자, v2 컬럼 50자 이내)을 통과하고 정규화 값이 v2와 이번 실행에서 겹치지 않을 때만
     * 쓴다. 쓰지 못하면 앱에서 온보딩할 때 새로 정한다.
     */
    private Nickname nickname(String raw, Set<String> takenNicknames) {
        Nickname nickname;
        try {
            nickname = Nickname.of(raw);
        }
        catch (MobileException exception) {
            return null;
        }
        if (!takenNicknames.add(nickname.normalized())) {
            return null;
        }
        return nickname;
    }

    private static String email(String value) {
        String email = blankToNull(value);
        if (email == null || email.length() > EMAIL_MAX_LENGTH) {
            return null;
        }
        return email;
    }

    private static String bio(String value) {
        if (value == null || value.isEmpty() || value.codePointCount(0, value.length()) > BIO_MAX_LENGTH) {
            return null;
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static boolean isProvider(String provider) {
        for (OAuthProvider candidate : OAuthProvider.values()) {
            if (candidate.name().equals(provider)) {
                return true;
            }
        }
        return false;
    }

}
