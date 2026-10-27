package com.raota.mobile.ramenlog.application.service;

import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade;
import com.raota.mobile.account.application.facade.MobileAccountRamenLogFacade.MobileAuthor;
import com.raota.mobile.common.error.MobileErrorCode;
import com.raota.mobile.common.error.MobileException;
import com.raota.mobile.ramenlog.application.result.MobileBlockedMemberResult;
import com.raota.mobile.ramenlog.domain.model.MobileUserBlock;
import com.raota.mobile.ramenlog.domain.repository.MobileUserBlockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차단과 해제는 이미 그 상태여도 성공한다. 차단한 회원의 글과 댓글은 피드와 댓글 목록에서 빠진다. 활성 회원만 차단하고 보여줘, 탈퇴 유예·정지·탈퇴한
 * 회원은 없는 회원과 똑같이 404로 응답하고 목록에서 뺀다.
 */
@Service
@RequiredArgsConstructor
public class MobileUserBlockService {

    private final MobileUserBlockRepository blocks;

    private final MobileAccountRamenLogFacade accounts;

    private final Clock clock;

    @Transactional
    public void block(Long blockerId, Long memberId) {
        if (blockerId.equals(memberId)) {
            throw new MobileException(MobileErrorCode.VALIDATION_ERROR, "자기 자신은 차단할 수 없습니다.");
        }
        MobileAuthor member = accounts.authors(List.of(memberId)).get(memberId);
        if (member == null || !member.active()) {
            throw new MobileException(MobileErrorCode.RESOURCE_NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        blocks.insertIfAbsent(blockerId, memberId, Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
    }

    @Transactional
    public void unblock(Long blockerId, Long memberId) {
        blocks.deleteBlock(blockerId, memberId);
    }

    /** 최근에 차단한 순서다. */
    @Transactional(readOnly = true)
    public List<MobileBlockedMemberResult> blocked(Long blockerId) {
        List<Long> ids = blocks.findByBlockerIdOrderByCreatedAtDescBlockedIdDesc(blockerId).stream()
                .map(MobileUserBlock::getBlockedId)
                .toList();
        Map<Long, MobileAuthor> members = accounts.authors(ids);
        List<MobileBlockedMemberResult> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            MobileAuthor member = members.get(id);
            if (member != null && member.active()) {
                result.add(new MobileBlockedMemberResult(id.toString(), member.nickname(), member.avatarUrl()));
            }
        }
        return result;
    }
}
