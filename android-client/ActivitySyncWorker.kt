package com.kovanlabs.wellness.client

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Background worker to sync Health Connect data to the Wellness Service API.
 */
class ActivitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
    private val apiToken: String // Retrieved from secure storage
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Sync data for the last 24 hours
            val endTime = Instant.now()
            val startTime = endTime.minus(24, ChronoUnit.HOURS)

            val activityData = healthConnectRepository.readActivityData(startTime, endTime)

            // Skip sync if no steps recorded
            if (activityData.steps == 0L) {
                return@withContext Result.success()
            }

            // Prepare JSON payload
            val jsonPayload = JSONObject().apply {
                put("steps", activityData.steps)
                put("distanceMeters", activityData.distanceMeters)
                put("caloriesBurned", activityData.caloriesBurned)
                put("startTime", activityData.startTime.toString())
                put("endTime", activityData.endTime.toString())
            }

            // Sync to backend
            val url = URL("http://10.0.2.2:8080/api/activities/sync") // Local emulator alias
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiToken")
            connection.doOutput = true

            connection.outputStream.use { os ->
                val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
