package com.kovanlabs.wellness.dto.team;

import com.kovanlabs.wellness.entity.enums.TeamMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberResponse {

    private Long id;

    private Long userId;

    private String fullName;

    private String email;

    private TeamMemberRole role;

    private Instant joinedAt;
}
