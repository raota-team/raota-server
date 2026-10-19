package com.raota.global.presentation.file;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.global.file.UploadTicketRateLimiter;
import com.raota.support.BaseIntegrationTest;
import com.raota.web.account.domain.member.model.MemberProfile;
import com.raota.web.account.domain.member.model.MemberRole;
import com.raota.web.account.domain.member.repository.MemberRepository;
import com.raota.web.account.infrastructure.auth.JwtTokenProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 웹(v1) 업로드 티켓: 회원별 1시간 발급 한도와 관리자 예외(#84). */
class FIleControllerTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository members;

    @Autowired
    private JwtTokenProvider tokens;

    @Autowired
    private StringRedisTemplate redis;

    private MockMvc mvc;

    private final List<Long> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanUp() {
        for (Long memberId : created) {
            redis.delete(redis.keys("upload-ticket:v1:" + memberId + ":*"));
        }
        members.deleteAllById(created);
    }

    @Test
    void 회원은_한_시간에_30장까지_받고_그다음은_429다() throws Exception {
        String token = token(MemberRole.USER);
        for (int ticket = 0; ticket < UploadTicketRateLimiter.FILES_PER_HOUR; ticket++) {
            ticket(token, "BACKGROUND").andExpect(status().isOk()).andExpect(jsonPath("$.img_url").isNotEmpty());
        }

        ticket(token, "COMMUNITY").andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 관리자는_매장_사진을_한도_없이_받는다() throws Exception {
        String token = token(MemberRole.ADMIN);
        for (int ticket = 0; ticket <= UploadTicketRateLimiter.FILES_PER_HOUR; ticket++) {
            ticket(token, "SHOP").andExpect(status().isOk());
        }
    }

    @Test
    void 로그인하지_않으면_받을_수_없다() throws Exception {
        mvc.perform(get("/files/upload-ticket").param("type", "PROFILE").param("extension", "webp"))
            .andExpect(status().isUnauthorized());
    }

    private ResultActions ticket(String token, String type) throws Exception {
        return mvc.perform(get("/files/upload-ticket").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .param("type", type)
            .param("extension", "webp")
            .param("contentType", "image/webp"));
    }

    private String token(MemberRole role) {
        MemberProfile member = members.saveAndFlush(MemberProfile.builder().nickname("업로드 회원").role(role).build());
        created.add(member.getId());
        return tokens.createAccessToken(member.getId());
    }

}
