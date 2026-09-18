package com.kovanlabs.wellness.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIChatRequest {

    private Long conversationId;

    @NotBlank(message = "Prompt is required")
    private String prompt;
}
