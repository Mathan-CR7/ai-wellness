package com.kovanlabs.wellness.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kovanlabs.wellness.model.HealthConnectErrorCode
import com.kovanlabs.wellness.model.HealthConnectState
import java.time.Instant

/**
 * Manages official Android Health Connect client SDK interactions.
 * Strictly reads genuine user data from Health Connect's StepsRecord.
 * Never fabricates or mocks step data.
 */
class HealthConnectManager(private val context: Context) {

    val REQUIRED_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    fun isHealthConnectAvailable(): Boolean {
        return getSdkStatus() == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissionsGranted(): Boolean {
        if (!isHealthConnectAvailable()) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
    }

    suspend fun readGenuineStepData(startTime: Instant, endTime: Instant): HealthConnectState {
        if (!isHealthConnectAvailable()) {
            return HealthConnectState.StatusError(
                HealthConnectErrorCode.HEALTH_CONNECT_UNAVAILABLE,
                "Android Health Connect SDK is not supported or not installed on this device."
            )
        }

        if (!hasPermissionsGranted()) {
            return HealthConnectState.StatusError(
                HealthConnectErrorCode.PERMISSION_REQUIRED,
                "Health Connect READ_STEPS permission is required to read activity data."
            )
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val timeFilter = TimeRangeFilter.between(startTime, endTime)

            // Use Health Connect's official Aggregate API to get deduplicated total matching Health Connect UI
            val aggregateResponse = client.aggregate(
                androidx.health.connect.client.request.AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = timeFilter
                )
            )

            val totalSteps = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L
            val totalDistanceMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            val totalCalories = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            if (totalSteps == 0L) {
                HealthConnectState.StatusError(
                    HealthConnectErrorCode.NO_DATA_AVAILABLE,
                    "No step records exist in Health Connect for the queried time range."
                )
            } else {
                HealthConnectState.DataRetrieved(
                    stepCount = totalSteps.toInt(),
                    distanceMeters = totalDistanceMeters,
                    caloriesBurned = totalCalories,
                    startTime = startTime.toString(),
                    endTime = endTime.toString()
                )
            }
        } catch (e: Exception) {
            HealthConnectState.StatusError(
                HealthConnectErrorCode.SYNC_FAILED,
                e.message ?: "Failed to query Health Connect records"
            )
        }
    }
}
