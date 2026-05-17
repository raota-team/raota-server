package com.raota.infrastructure.log;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbedField {
    private String name;
    private String value;
    private boolean inline; // true면 필드들이 가로로 나란히 배치됨
}
