package com.kovanlabs.wellness.model

import com.google.gson.annotations.SerializedName

data class AIChatRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("conversationId") val conversationId: Long? = null
)

data class AIChatResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("timestamp") val timestamp: String
)
