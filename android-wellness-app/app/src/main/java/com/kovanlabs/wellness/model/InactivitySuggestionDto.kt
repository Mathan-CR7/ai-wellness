package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class InactivitySuggestionDto(
    @SerializedName("userId") val userId: Long?,
    @SerializedName("userEmail") val userEmail: String?,
    @SerializedName("suggestion") val suggestion: String,
    @SerializedName("inactivityMinutes") val inactivityMinutes: Long?,
    @SerializedName("currentSteps") val currentSteps: Long?,
    @SerializedName("timestamp") val timestamp: String?
)
