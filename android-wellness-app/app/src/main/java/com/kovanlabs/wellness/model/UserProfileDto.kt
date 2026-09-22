package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class UserProfileDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("dailyStepGoal") val dailyStepGoal: Int?,
    @SerializedName("weightKg") val weightKg: Double?,
    @SerializedName("heightCm") val heightCm: Double?
)

data class UpdateProfileRequestDto(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("dailyStepGoal") val dailyStepGoal: Int?,
    @SerializedName("weightKg") val weightKg: Double?,
    @SerializedName("heightCm") val heightCm: Double?
)
