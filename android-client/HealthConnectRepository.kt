package com.kovanlabs.wellness.client

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * Repository to read physical activity data from Android Health Connect.
 */
class HealthConnectRepository(private val healthConnectClient: HealthConnectClient) {

    suspend fun readActivityData(startTime: Instant, endTime: Instant): ActivityData {
        val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)

        // Read Steps
        val stepsRequest = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val stepsResponse = healthConnectClient.readRecords(stepsRequest)
        val totalSteps = stepsResponse.records.sumOf { it.count }

        // Read Distance
        val distanceRequest = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val distanceResponse = healthConnectClient.readRecords(distanceRequest)
        val totalDistance = distanceResponse.records.sumOf { it.distance.inMeters }

        // Read Calories
        val caloriesRequest = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val caloriesResponse = healthConnectClient.readRecords(caloriesRequest)
        val totalCalories = caloriesResponse.records.sumOf { it.energy.inKilocalories }

        return ActivityData(
            steps = totalSteps,
            distanceMeters = totalDistance,
            caloriesBurned = totalCalories,
            startTime = startTime,
            endTime = endTime
        )
    }
}

data class ActivityData(
    val steps: Long,
    val distanceMeters: Double,
    val caloriesBurned: Double,
    val startTime: Instant,
    val endTime: Instant
)
