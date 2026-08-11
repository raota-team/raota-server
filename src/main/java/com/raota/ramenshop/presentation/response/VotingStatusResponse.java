package com.raota.ramenshop.presentation.response;

import com.raota.ramenshop.presentation.response.VoteResultsDto;
import java.util.List;

public record VotingStatusResponse (
        long total_votes,
        List<VoteResultsDto> vote_results
){
}
