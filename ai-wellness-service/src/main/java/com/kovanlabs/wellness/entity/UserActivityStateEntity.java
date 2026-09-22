package com.kovanlabs.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_activity_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "last_activity_time", nullable = false)
    private Instant lastActivityTime;

    @Column(name = "last_step_count", nullable = false)
    private Long lastStepCount;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent;

    @Column(name = "notification_sent_at")
    private Instant notificationSentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.notificationSent == null) {
            this.notificationSent = false;
        }
        if (this.lastStepCount == null) {
            this.lastStepCount = 0L;
        }
        if (this.lastActivityTime == null) {
            this.lastActivityTime = Instant.now();
        }
    }
}
