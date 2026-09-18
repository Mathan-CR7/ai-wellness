package com.kovanlabs.wellness.dto.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {

    private Long id;

    private String name;

    private String description;

    private String inviteCode;

    private Long ownerId;

    private Integer memberCount;

    private Instant createdAt;
}
