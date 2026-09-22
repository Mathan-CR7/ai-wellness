package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class ExerciseLogRequestDto(
    @SerializedName("exerciseType") val exerciseType: String,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("caloriesBurned") val caloriesBurned: Int,
    @SerializedName("notes") val notes: String? = null
)

data class ExerciseResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("exerciseType") val exerciseType: String,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("caloriesBurned") val caloriesBurned: Int,
    @SerializedName("notes") val notes: String?,
    @SerializedName("loggedAt") val loggedAt: String?
)
