package com.raota.ramenshop.application.service;

import com.raota.account.domain.member.model.MemberProfile;
import com.raota.account.domain.member.repository.MemberRepository;
import com.raota.ramenshop.presentation.response.VotingStatusResponse;
import com.raota.ramenshop.presentation.response.VoteResultsDto;
import com.raota.ramenshop.domain.model.NormalMenu;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.ramenshop.domain.model.MenuVote;
import com.raota.ramenshop.domain.repository.MenuVoteRepository;
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

        // 현재 유저가 투표한 메뉴 찾기 (취소되지 않은 활성 투표만)
        Optional<MenuVote> userVote = findActiveVote(memberId, shopId);

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
        Optional<MenuVote> existingVote = findExistingVote(memberId, shopId);

        if (existingVote.isPresent()) {
            MenuVote vote = existingVote.get();
            
            // 이미 투표된 상태인 경우
            if (!vote.isCancelled()) {
                // 같은 메뉴를 다시 누른 경우 -> 투표 취소
                if (vote.getNormalMenu().getId().equals(menuId)) {
                    vote.cancel();
                } 
                // 다른 메뉴를 누른 경우 -> 기존 투표 메뉴 변경
                else {
                    RamenShop ramenShop = ramenShopRepository.findById(shopId)
                            .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 라멘가게 입니다."));
                    NormalMenu newMenu = ramenShop.getNormalMenus().findMenuById(menuId)
                            .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 메뉴입니다."));
                    vote.reVote(newMenu);
                }
            } 
            // 이전에 투표했다가 취소한 상태인 경우 -> 다시 활성화 및 메뉴 설정
            else {
                RamenShop ramenShop = ramenShopRepository.findById(shopId)
                        .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 라멘가게 입니다."));
                NormalMenu newMenu = ramenShop.getNormalMenus().findMenuById(menuId)
                        .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 메뉴입니다."));
                vote.reVote(newMenu);
            }
        } else {
            // 투표 기록이 아예 없었던 경우 -> 새 투표 생성
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
        MemberProfile member = null;
        if (memberId != null) {
            member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("찾을수 없는 유저입니다."));
        }

        MenuVote vote = MenuVote.builder()
                .memberProfile(member)
                .ramenShop(ramenShop)
                .normalMenu(menu)
                .build();

        voteRepository.save(vote);
    }

    private Optional<MenuVote> findExistingVote(Long memberId, Long shopId) {
        return voteRepository.findByMemberProfileIdAndRamenShopId(memberId, shopId);
    }

    private Optional<MenuVote> findActiveVote(Long memberId, Long shopId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return voteRepository.findByMemberProfileIdAndRamenShopIdAndIsCancelledFalse(memberId, shopId);
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
