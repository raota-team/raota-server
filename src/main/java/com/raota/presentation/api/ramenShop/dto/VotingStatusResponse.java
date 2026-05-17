package com.raota.presentation.api.ramenShop.dto;

import com.raota.presentation.api.ramenShop.dto.VoteResultsDto;
import java.util.List;

public record VotingStatusResponse (
        long total_votes,
        List<VoteResultsDto> vote_results
){
}
