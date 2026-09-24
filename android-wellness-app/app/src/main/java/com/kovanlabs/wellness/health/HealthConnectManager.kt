package com.kovanlabs.wellness.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kovanlabs.wellness.model.HealthConnectErrorCode
import com.kovanlabs.wellness.model.HealthConnectState
import java.time.Instant

/**
 * Manages official Android Health Connect client SDK interactions.
 * Strictly reads genuine user data from Health Connect's StepsRecord.
 * Never fabricates or mocks step data.
 *
 * DUAL-STRATEGY READING:
 * 1. PRIMARY: Aggregate API (deduplicated, matches Health Connect UI)
 * 2. FALLBACK: Raw StepsRecord records — written in real-time as you walk,
 *    unlike the aggregate which can lag or only finalize at end-of-day
 *    on some Android devices and health apps (Google Fit, Samsung Health, etc.)
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

            // ── STRATEGY 1: Aggregate API ────────────────────────────────────
            // Returns deduplicated total matching the Health Connect UI.
            // On some devices/apps this only finalizes at end-of-day, so we
            // always try the raw record fallback when this returns 0.
            val aggregateResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = timeFilter
                )
            )

            var totalSteps       = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L
            var totalDistMeters  = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            var totalCalories    = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            // ── STRATEGY 2: Raw StepsRecord fallback ─────────────────────────
            // Individual StepsRecord entries are written in real-time as you walk
            // (each walking session → one record). This captures steps that the
            // aggregate hasn't finalized yet, ensuring real-time data all day.
            if (totalSteps == 0L) {
                val rawStepRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = timeFilter
                    )
                )
                val rawSteps = rawStepRecords.records.sumOf { it.count }

                if (rawSteps > 0L) {
                    totalSteps = rawSteps

                    // Also read raw distance and calories since aggregate was empty
                    val rawDistRecords = client.readRecords(
                        ReadRecordsRequest(
                            recordType = DistanceRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    totalDistMeters = rawDistRecords.records.sumOf { it.distance.inMeters }

                    val rawCalRecords = client.readRecords(
                        ReadRecordsRequest(
                            recordType = TotalCaloriesBurnedRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    totalCalories = rawCalRecords.records.sumOf { it.energy.inKilocalories }
                }
            }

            // ── ALWAYS return DataRetrieved — even when steps == 0 ───────────
            // Previously, returning StatusError for 0 caused the sync loop in
            // MainActivity to SKIP the POST /api/steps/sync call entirely,
            // so the database was never updated during the day. Steps only appeared
            // in the web dashboard after the day ended (Health Connect finalized data).
            HealthConnectState.DataRetrieved(
                stepCount        = totalSteps.toInt(),
                distanceMeters   = totalDistMeters,
                caloriesBurned   = totalCalories,
                startTime        = startTime.toString(),
                endTime          = endTime.toString()
            )

        } catch (e: Exception) {
            HealthConnectState.StatusError(
                HealthConnectErrorCode.SYNC_FAILED,
                e.message ?: "Failed to query Health Connect records"
            )
        }
    }
}
