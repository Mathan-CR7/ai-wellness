package com.kovanlabs.wellness.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"message", "prompt"})
    private String prompt;
}
