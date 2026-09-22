package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class ActivityTrendDto(
    @SerializedName("sevenDayAverageSteps") val sevenDayAverageSteps: Double,
    @SerializedName("goalCompletionRatePercentage") val goalCompletionRatePercentage: Double,
    @SerializedName("activeStreakDays") val activeStreakDays: Int,
    @SerializedName("activeDaysInPeriod") val activeDaysInPeriod: Int
)
