package com.raota.presentation.api.ramenShop.response;

import com.raota.presentation.api.ramenShop.response.VoteResultsDto;
import java.util.List;

public record VotingStatusResponse (
        long total_votes,
        List<VoteResultsDto> vote_results
){
}
