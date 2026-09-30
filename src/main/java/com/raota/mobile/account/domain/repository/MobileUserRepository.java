package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MobileUserRepository extends JpaRepository<MobileUser, Long> {

    boolean existsByNicknameNormalizedAndIdNot(String nicknameNormalized, Long id);

    /** 같은 회원의 온보딩 완료 요청을 직렬화해 동의를 중복 기록하지 않는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from MobileUser user where user.id = :id")
    Optional<MobileUser> findByIdForUpdate(Long id);

}
