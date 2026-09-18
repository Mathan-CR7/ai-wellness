package com.kovanlabs.wellness.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "step_count", nullable = false)
    private Integer stepCount;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "calories_burned", nullable = false)
    private Double caloriesBurned;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "source_device", nullable = false, length = 100)
    private String sourceDevice;

    @Column(name = "synced_at", nullable = false, updatable = false)
    private Instant syncedAt;

    @PrePersist
    protected void onCreate() {
        this.syncedAt = Instant.now();
    }
}
