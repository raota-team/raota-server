package com.raota.web.ramenshop.presentation.response;

import java.util.List;

public record VotingStatusResponse(long total_votes, List<VoteResultsDto> vote_results) {}
