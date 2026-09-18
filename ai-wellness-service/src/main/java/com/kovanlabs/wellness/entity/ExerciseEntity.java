package com.kovanlabs.wellness.entity;

import com.kovanlabs.wellness.entity.enums.ExerciseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 50)
    private ExerciseType exerciseType;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "avg_heart_rate")
    private Integer avgHeartRate;

    @Column(name = "calories_burned", nullable = false)
    private Double caloriesBurned;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
