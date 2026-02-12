package com.raota.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.ramenShop.dto.VoteResultsDto;
import com.raota.domain.ramenShop.model.Address;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.vote.model.MenuVote;
import com.raota.domain.vote.repository.MenuVoteRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestDataHelper.class)
public class VoteRepositoryTest {

    @Autowired
    private MenuVoteRepository voteRepository;

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private EntityManager em;

    @DisplayName("가게의 투표 현황을 본다.")
    @Test
    void findMenuVoteCounts_by_shopId() {
        // given
        // 1) 회원 2명
        MemberProfile member1 = testDataHelper.createMember("유저1");
        MemberProfile member2 = testDataHelper.createMember("유저2");

        // 2) 가게 생성
        RamenShop shop = testDataHelper.createRamenShop("테스트 라멘집", new Address("서울","마포구","망원동","123"));

        // 3) 메뉴 4개 생성
        NormalMenu menu1 = NormalMenu.builder()
                .name("시오라멘")
                .price(9000)
                .isSignature(true)
                .imageUrl("https://cdn.men.com/salt.jpg")
                .ramenShop(shop)
                .build();

        NormalMenu menu2 = NormalMenu.builder()
                .name("니보시 츠케멘")
                .price(12000)
                .isSignature(false)
                .imageUrl("https://cdn.men.com/niboshi.jpg")
                .ramenShop(shop)
                .build();

        NormalMenu menu3 = NormalMenu.builder()
                .name("카라구치 라멘")
                .price(10000)
                .isSignature(false)
                .imageUrl("https://cdn.men.com/spicy.jpg")
                .ramenShop(shop)
                .build();

        NormalMenu menu4 = NormalMenu.builder()
                .name("차슈동")
                .price(8000)
                .isSignature(false)
                .imageUrl("https://cdn.men.com/chashu.jpg")
                .ramenShop(shop)
                .build();

        shop.addNormalMenu(menu1);
        shop.addNormalMenu(menu2);
        shop.addNormalMenu(menu3);
        shop.addNormalMenu(menu4);

        em.flush();

        // 4) 투표 데이터 생성
        saveVote(menu1, member1);
        saveVote(menu1, member2);
        saveVote(menu1, member1);

        saveVote(menu2, member1);
        saveVote(menu2, member2);

        saveVote(menu3, member1);


        em.flush();
        em.clear();

        // when
        List<VoteResultsDto> results = voteRepository.findMenuVoteCounts(shop.getId());

        // then
        assertThat(results).hasSize(4);

        VoteResultsDto r1 = results.getFirst();
        assertThat(r1.getMenu_id()).isEqualTo(menu1.getId());
        assertThat(r1.getMenu_name()).isEqualTo("시오라멘");
        assertThat(r1.getVote_count()).isEqualTo(3L);

        VoteResultsDto r2 = results.get(1);
        assertThat(r2.getMenu_id()).isEqualTo(menu2.getId());
        assertThat(r2.getMenu_name()).isEqualTo("니보시 츠케멘");
        assertThat(r2.getVote_count()).isEqualTo(2L);

        VoteResultsDto r3 = results.get(2);
        assertThat(r3.getMenu_id()).isEqualTo(menu3.getId());
        assertThat(r3.getMenu_name()).isEqualTo("카라구치 라멘");
        assertThat(r3.getVote_count()).isEqualTo(1L);

        VoteResultsDto r4 = results.get(3);
        assertThat(r4.getMenu_id()).isEqualTo(menu4.getId());
        assertThat(r4.getMenu_name()).isEqualTo("차슈동");
        assertThat(r4.getVote_count()).isEqualTo(0L);
    }

    private void saveVote(NormalMenu menu, MemberProfile member) {
        MenuVote vote = MenuVote.builder()
                .normalMenu(menu)
                .memberProfile(member)
                .build();
        voteRepository.save(vote);
    }
}
