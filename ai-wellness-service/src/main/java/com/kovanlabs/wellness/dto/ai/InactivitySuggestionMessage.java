package com.kovanlabs.wellness.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InactivitySuggestionMessage {
    private Long userId;
    private String userEmail;
    private String suggestion;
    private long inactivityMinutes;
    private long currentSteps;
    private Instant timestamp;
}
