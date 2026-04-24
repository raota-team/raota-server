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
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MenuVoteService {

    private final MenuVoteRepository voteRepository;
    private final RamenShopRepository ramenShopRepository;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;

    public VotingStatusResponse getVotingStatus(Long shopId, Long memberId) {
        List<VoteResultsDto> statusDto = voteRepository.findMenuVoteCounts(shopId);
        long totalCount = getTotalCount(statusDto);

        // 현재 유저가 투표한 메뉴 찾기
        Optional<MenuVote> userVote = (memberId != null) 
                ? voteRepository.findByMemberProfileIdAndRamenShopId(memberId, shopId)
                : Optional.empty();

        statusDto.forEach(dto -> {
            double percentage = calculatePercentage(totalCount, dto);
            dto.setPercentage(percentage);
            
            // 유저가 이 메뉴에 투표했는지 설정
            if (userVote.isPresent() && userVote.get().getNormalMenu().getId().equals(dto.getMenuId())) {
                dto.setVoted(true);
            }
        });

        return new VotingStatusResponse(
                totalCount,
                statusDto
        );
    }

    @Transactional
    public VotingStatusResponse voteTheMenu(Long shopId, Long menuId, Long memberId) {
        Optional<MenuVote> existingVote = voteRepository.findByMemberProfileIdAndRamenShopId(memberId, shopId);

        if (existingVote.isPresent()) {
            MenuVote vote = existingVote.get();
            // 같은 메뉴를 다시 누른 경우 -> 투표 취소
            if (vote.getNormalMenu().getId().equals(menuId)) {
                voteRepository.delete(vote);
            } 
            // 다른 메뉴를 누른 경우 -> 기존 투표 삭제 후 새 투표
            else {
                voteRepository.delete(vote);
                createNewVote(shopId, menuId, memberId);
            }
        } else {
            // 투표가 없었던 경우 -> 새 투표 생성
            createNewVote(shopId, menuId, memberId);
        }

        voteRepository.flush();
        entityManager.clear();

        return getVotingStatus(shopId, memberId);
    }

    private void createNewVote(Long shopId, Long menuId, Long memberId) {
        RamenShop ramenShop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 라멘가게 입니다."));
        NormalMenu menu = ramenShop.getNormalMenus().findMenuById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 메뉴입니다."));
        MemberProfile member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 유저입니다."));

        MenuVote vote = MenuVote.builder()
                .memberProfile(member)
                .ramenShop(ramenShop)
                .normalMenu(menu)
                .build();

        voteRepository.save(vote);
    }

    private long getTotalCount(List<VoteResultsDto> statusDto){
        return statusDto.stream()
                .mapToLong(VoteResultsDto::getVoteCount)
                .sum();
    }

    private Double calculatePercentage(long totalCount,VoteResultsDto dto){
        if (totalCount == 0L) {
            return 0.0;
        }

        double percentage = dto.getVoteCount()*100.0/totalCount;
        return Math.round(percentage * 100.0) / 100.0;
    }
}
