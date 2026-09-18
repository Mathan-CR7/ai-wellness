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
    val email: String
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
