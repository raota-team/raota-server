package com.raota.mobile.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.global.file.cleanup.StoredImageReferences;
import com.raota.mobile.account.domain.model.MobileUser;
import com.raota.mobile.account.domain.model.Nickname;
import com.raota.mobile.account.domain.repository.MobileUserRepository;
import com.raota.support.BaseIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MobileProfileStoredImagesIntegrationTest extends BaseIntegrationTest {

    private static final Instant AT = Instant.parse("2026-09-01T00:00:00Z");

    private static final String BASE = "https://images.raota.net/v2/profiles/";

    @Autowired
    private MobileProfileStoredImages references;

    @Autowired
    private List<StoredImageReferences> contributors;

    @Autowired
    private MobileUserRepository users;

    private Long activeId;

    private Long withdrawingId;

    @BeforeEach
    void setUp() {
        MobileUser active = MobileUser.onboarding("stored-images-profile@example.com");
        active.completeOnboarding(Nickname.of("StoredProf"), AT);
        active.updateProfile(null, BASE + "active.png", null, null);
        activeId = users.saveAndFlush(active).getId();
        MobileUser withdrawing = MobileUser.onboarding("stored-images-withdrawing@example.com");
        withdrawing.completeOnboarding(Nickname.of("StoredLeave"), AT);
        withdrawing.updateProfile(null, BASE + "withdrawing.png", null, null);
        withdrawing.requestWithdrawal(AT, Duration.ofDays(30));
        withdrawingId = users.saveAndFlush(withdrawing).getId();
    }

    @AfterEach
    void cleanUp() {
        users.deleteById(activeId);
        users.deleteById(withdrawingId);
    }

    @Test
    void 프로필_접두어를_맡고_탈퇴_유예_회원의_사진도_참조로_본다() {
        assertThat(contributors).contains(references);
        assertThat(references.prefix()).isEqualTo("v2/profiles/");

        assertThat(references.referencedUrls(List.of(
                        BASE + "active.png", BASE + "withdrawing.png", BASE + "replaced.png", BASE + "active.png")))
                .containsExactlyInAnyOrder(BASE + "active.png", BASE + "withdrawing.png");
    }
}
