package com.raota.mobile.account.application.facade;

import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.MobileUserStatus;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 작성자 표시 정보와 회원 기록 수를 회원 모듈에서 제공한다. */
@Service
@RequiredArgsConstructor
public class MobileAccountRamenLogFacade {

    private final MobileUserRepository users;

    public Map<Long, MobileAuthor> authors(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MobileAuthor> result = new HashMap<>();
        for (MobileUser user : users.findAllById(userIds)) {
            result.put(user.getId(), new MobileAuthor(user.getId(), user.getNickname(), user.getAvatarUrl(),
                    user.getLogCount(), user.getStatus() == MobileUserStatus.ACTIVE));
        }
        return result;
    }

    @Transactional
    public void incrementLogCount(Long userId) {
        users.incrementLogCount(userId);
    }

    @Transactional
    public void decrementLogCount(Long userId) {
        users.decrementLogCount(userId);
    }

    public record MobileAuthor(Long id, String nickname, String avatarUrl, int logCount, boolean active) {
    }

}
