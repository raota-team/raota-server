package com.raota.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.ramenShop.controller.request.RamenShopSearchRequest;
import com.raota.domain.ramenShop.controller.response.StoreSummaryResponse;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDataHelper.class)
public class RamenShopRepositoryTest {

    @Autowired
    private RamenShopRepository ramenShopRepository;

    @Autowired
    private TestDataHelper testDataHelper;

    private MemberProfile memberProfile;
    private MemberProfile memberProfile2;
    private RamenShop ramenShop;

    @BeforeEach
    void setUp(){
        memberProfile = testDataHelper.createMember("테스트");
        memberProfile2 = testDataHelper.createMember("테스트2");
        ramenShop = testDataHelper.createRamenShop("테스트 라멘샵",new Address("서울시","마포구","망원동","123"));
    }

    @DisplayName("조건에 맞는 가게의 목록을 가져온다.")
    @Test
    void get_a_list_of_stores(){
        testDataHelper.createRamenShop("테스트 라멘샵",new Address("서울","마포구","망원동","123"));
        testDataHelper.createRamenShop("라멘스키 강남점", new Address("서울", "강남구", "역삼동", "1-1"));
        testDataHelper.createRamenShop("라멘스키 홍대점", new Address("서울", "마포구", "서교동", "2-1"));
        testDataHelper.createRamenShop("하쿠텐 망원점", new Address("서울", "마포구", "망원동", "3-1"));
        testDataHelper.createRamenShop("멘야산다이메 합정점", new Address("서울", "마포구", "합정동", "4-1"));
        testDataHelper.createRamenShop("부탄츄 신촌점", new Address("서울", "서대문구", "신촌동", "5-1"));

        RamenShopSearchRequest request = new RamenShopSearchRequest("서울 강남구", null);

        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<StoreSummaryResponse> result = ramenShopRepository.searchStores(request.getRegion(), request.getKeyword(), pageRequest);

        StoreSummaryResponse first = result.getContent().getFirst();
        assertThat(first.name()).isEqualTo("라멘스키 강남점");
        assertThat(first.address()).contains("서울 강남구");
        assertThat(first.tagsAsList()).contains("아부라소바");
    }

}
