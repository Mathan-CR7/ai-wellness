package com.kovanlabs.wellness.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kovanlabs.wellness.api.AuthInterceptor
import com.kovanlabs.wellness.api.WellnessApiService
import com.kovanlabs.wellness.health.HealthConnectManager
import com.kovanlabs.wellness.model.ActivitySyncPayload
import com.kovanlabs.wellness.model.HealthConnectErrorCode
import com.kovanlabs.wellness.model.HealthConnectState
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Android WorkManager CoroutineWorker for background sync.
 * Syncs ONLY genuine Health Connect step records.
 * Never fabricates or mocks data.
 */
class ActivitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val healthConnectManager = HealthConnectManager(appContext)

    private val apiService: WellnessApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(appContext))
            .build()

        val baseUrl = appContext.getSharedPreferences("wellness_config", Context.MODE_PRIVATE)
            .getString("backend_url", "https://ai-wellness-jt1d.onrender.com/") ?: "https://ai-wellness-jt1d.onrender.com/"

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WellnessApiService::class.java)
    }

    override suspend fun doWork(): Result {
        // 1. Verify Health Connect availability and permissions
        if (!healthConnectManager.isHealthConnectAvailable() || !healthConnectManager.hasPermissionsGranted()) {
            return Result.failure()
        }

        val endTime = Instant.now()
        val startTime = endTime.minus(1, ChronoUnit.HOURS)

        // 2. Read genuine step data
        return when (val state = healthConnectManager.readGenuineStepData(startTime, endTime)) {
            is HealthConnectState.DataRetrieved -> {
                val payload = ActivitySyncPayload(
                    stepCount = state.stepCount,
                    distanceMeters = state.distanceMeters,
                    caloriesBurned = state.caloriesBurned,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    sourceDevice = "Android Health Connect (WorkManager Background Sync)"
                )

                try {
                    val response = apiService.syncActivity(payload)
                    if (response.isSuccessful) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                } catch (e: Exception) {
                    Result.retry()
                }
            }
            is HealthConnectState.StatusError -> {
                if (state.code == HealthConnectErrorCode.NO_DATA_AVAILABLE) {
                    // Genuine no data state — do not fabricate steps
                    Result.success()
                } else {
                    Result.failure()
                }
            }
            else -> Result.failure()
        }
    }
}
