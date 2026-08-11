package com.raota.acceptance.presentation.admin.ramenShop;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.model.MemberRole;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.model.RamenShopReport;
import com.raota.domain.ramenShop.model.RamenShopReportType;
import com.raota.domain.ramenShop.repository.RamenShopReportRepository;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.account.infrastructure.auth.JwtTokenProvider;
import com.raota.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class RamenShopReportAdminControllerTest extends BaseIntegrationTest {

    private MockMvc mockMvc;
    private String adminToken;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private RamenShopReportRepository ramenShopReportRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        MemberProfile admin = memberRepository.saveAndFlush(MemberProfile.builder()
                .nickname("관리자")
                .role(MemberRole.ADMIN)
                .build());
        adminToken = jwtTokenProvider.createAccessToken(admin.getId());
    }

    @Test
    void 라멘집_제보_목록을_JSON으로_조회하고_필터링한다() throws Exception {
        MemberProfile reporter = memberRepository.save(MemberProfile.builder()
                .nickname("제보회원")
                .email("reporter@example.com")
                .build());
        RamenShop closedShop = ramenShopRepository.save(RamenShop.builder()
                .name("폐업 라멘집")
                .branchName("성수점")
                .build());
        RamenShop otherShop = ramenShopRepository.save(RamenShop.builder()
                .name("영업중 라멘집")
                .build());
        RamenShopReport closedReport = ramenShopReportRepository.save(RamenShopReport.create(
                closedShop,
                reporter,
                RamenShopReportType.CLOSED,
                "매장이 폐업했습니다."
        ));
        ramenShopReportRepository.save(RamenShopReport.create(
                otherShop,
                reporter,
                RamenShopReportType.MENU_INFO_ERROR,
                "메뉴 가격이 달라요."
        ));

        mockMvc.perform(get("/admin/api/ramen-shop-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("keyword", "폐업")
                        .param("reportType", "CLOSED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(closedReport.getId()))
                .andExpect(jsonPath("$.data.items[0].shopName").value("폐업 라멘집"))
                .andExpect(jsonPath("$.data.items[0].memberEmail").value("reporter@example.com"))
                .andExpect(jsonPath("$.data.items[0].reportType").value("CLOSED"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }
}
