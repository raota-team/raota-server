package com.raota.infrastructure.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordEmbed {
    private String title;
    private String description;
    private Integer color;
    private String timestamp;
    private List<EmbedField> fields;
}
