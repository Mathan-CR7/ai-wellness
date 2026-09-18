package com.kovanlabs.wellness.model

enum class HealthConnectErrorCode {
    HEALTH_CONNECT_UNAVAILABLE,
    PERMISSION_REQUIRED,
    NO_DATA_AVAILABLE,
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    SYNC_FAILED
}

sealed class HealthConnectState {
    object HealthConnectAvailable : HealthConnectState()
    object PermissionGranted : HealthConnectState()
    
    data class StatusError(val code: HealthConnectErrorCode, val message: String) : HealthConnectState()
    
    data class DataRetrieved(
        val stepCount: Int,
        val distanceMeters: Double,
        val caloriesBurned: Double,
        val startTime: String,
        val endTime: String
    ) : HealthConnectState()

    data class DataUnchanged(val stepCount: Int) : HealthConnectState()
}
