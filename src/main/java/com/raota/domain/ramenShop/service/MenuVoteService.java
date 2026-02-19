package com.raota.domain.ramenShop.service;

import com.raota.domain.member.model.MemberProfile;
import com.raota.domain.member.repository.MemberRepository;
import com.raota.domain.ramenShop.controller.response.VotingStatusResponse;
import com.raota.domain.ramenShop.dto.VoteResultsDto;
import com.raota.domain.ramenShop.model.NormalMenu;
import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.ramenShop.model.MenuVote;
import com.raota.domain.ramenShop.repository.MenuVoteRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MenuVoteService {

    private final MenuVoteRepository voteRepository;
    private final RamenShopRepository ramenShopRepository;
    private final MemberRepository memberRepository;

    public VotingStatusResponse getVotingStatus(Long shopId) {
        List<VoteResultsDto> statusDto = voteRepository.findMenuVoteCounts(shopId);
        long totalCount = getTotalCount(statusDto);

        statusDto.forEach(dto->{
            double percentage = calculatePercentage(totalCount,dto);
            dto.setPercentage(percentage);
        });

        return new VotingStatusResponse(
                totalCount,
                statusDto
        );
    }

    @Transactional
    public VotingStatusResponse voteTheMenu(Long shopId, Long menuId, Long memberId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId)
                .orElseThrow(()-> new IllegalArgumentException("찾을수 없는 라멘가게 입니다."));
        NormalMenu menu = ramenShop.getNormalMenus().findMenuById(menuId)
                .orElseThrow(()-> new IllegalArgumentException("찾을수 없는 메뉴입니다."));
        MemberProfile member = memberRepository.findById(memberId)
                .orElseThrow(()-> new IllegalArgumentException("찾을수 없는 유저입니다."));

        MenuVote vote = MenuVote.builder()
                .memberProfile(member)
                .normalMenu(menu)
                .build();

        voteRepository.save(vote);
        return getVotingStatus(shopId);
    }

    private long getTotalCount(List<VoteResultsDto> statusDto){
        return statusDto.stream()
                .mapToLong(VoteResultsDto::getVote_count)
                .sum();
    }

    private Double calculatePercentage(long totalCount,VoteResultsDto dto){
        if (totalCount == 0L) {
            return 0.0;
        }

        double percentage = dto.getVote_count()*100.0/totalCount;
        return Math.round(percentage * 100.0) / 100.0;
    }
}
