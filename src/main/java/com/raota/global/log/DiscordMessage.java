package com.raota.global.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordMessage {
    private String content;
    private List<DiscordEmbed> embeds;
}
