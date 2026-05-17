package com.raota.presentation.api.ramenShop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class VoteResultsDto {
    @JsonProperty("menu_id")
    private Long menuId;

    @JsonProperty("menu_name")
    private String menuName;

    @JsonProperty("vote_count")
    private Long voteCount;

    @Setter
    private Double percentage;

    @Setter
    private boolean isVoted = false;

    public void toggleVoted(){
        if(isVoted){
            this.isVoted = false;
            return;
        }
        this.isVoted = true;
    }
}
