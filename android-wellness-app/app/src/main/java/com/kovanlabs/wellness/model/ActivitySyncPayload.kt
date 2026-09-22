package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class ActivitySyncPayload(
    @SerializedName("stepCount")
    val stepCount: Int,

    @SerializedName("distanceMeters")
    val distanceMeters: Double,

    @SerializedName("caloriesBurned")
    val caloriesBurned: Double,

    @SerializedName("startTime")
    val startTime: String,

    @SerializedName("endTime")
    val endTime: String,

    @SerializedName("sourceDevice")
    val sourceDevice: String
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("fullName")
    val fullName: String? = null
)

data class RegisterRequest(
    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class LeaderboardEntry(
    @SerializedName("rank")
    val rank: Int,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("fullName")
    val fullName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("totalSteps")
    val totalSteps: Long,

    @SerializedName("totalDistanceMeters")
    val totalDistanceMeters: Double,

    @SerializedName("totalCaloriesBurned")
    val totalCaloriesBurned: Double
)

data class TeamLeaderboardResponse(
    @SerializedName("teamId")
    val teamId: Long,

    @SerializedName("teamName")
    val teamName: String? = null,

    @SerializedName("rankings")
    val rankings: List<LeaderboardEntry> = emptyList()
)

data class ChallengeResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("targetSteps")
    val targetSteps: Long? = 10000L,

    @SerializedName("status")
    val status: String? = "ACTIVE"
)

data class ActivitySyncResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("stepCount")
    val stepCount: Int,

    @SerializedName("distanceMeters")
    val distanceMeters: Double,

    @SerializedName("caloriesBurned")
    val caloriesBurned: Double,

    @SerializedName("syncedAt")
    val syncedAt: String
)
