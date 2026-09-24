package com.raota.mobile.account.domain.repository;

import com.raota.mobile.account.domain.model.MobileUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileUserRepository extends JpaRepository<MobileUser, Long> {

}
