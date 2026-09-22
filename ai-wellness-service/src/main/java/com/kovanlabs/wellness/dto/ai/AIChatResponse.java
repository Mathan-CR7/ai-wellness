package com.kovanlabs.wellness.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIChatResponse {

    private Long conversationId;

    private Long messageId;

    private String content;

    private Instant timestamp;

    @JsonProperty("message")
    public String getMessage() {
        return content;
    }
}
