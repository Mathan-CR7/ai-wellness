package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class StepSyncRequestDto(
    @SerializedName("steps")
    val steps: Long,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("sourceDevice")
    val sourceDevice: String? = "Android Health Connect"
)

data class StepSyncResponseDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("date")
    val date: String,
    @SerializedName("steps")
    val steps: Long
)
