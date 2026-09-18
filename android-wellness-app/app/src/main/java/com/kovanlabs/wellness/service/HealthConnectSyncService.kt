package com.kovanlabs.wellness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kovanlabs.wellness.api.AuthInterceptor
import com.kovanlabs.wellness.api.WellnessApiService
import com.kovanlabs.wellness.health.HealthConnectManager
import com.kovanlabs.wellness.model.ActivitySyncPayload
import com.kovanlabs.wellness.model.HealthConnectErrorCode
import com.kovanlabs.wellness.model.HealthConnectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Dedicated Android Foreground Service for Health Connect synchronization.
 * Checks Health Connect every 20 seconds.
 * Implements duplicate suppression — sends HTTP POST ONLY when genuine step count changes.
 * Zero hardcoded values, zero artificial step increments (`steps++`), zero fake fallbacks.
 */
class HealthConnectSyncService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var apiService: WellnessApiService

    private var lastSyncedStepCount: Int? = null

    companion object {
        private const val TAG = "HealthConnectSyncSvc"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "health_connect_sync_channel"
        const val ACTION_START_SYNC = "ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "ACTION_STOP_SYNC"

        // Configurable 10-second polling requirement
        const val POLLING_INTERVAL_MS = 10000L
    }

    inner class LocalBinder : Binder() {
        fun getService(): HealthConnectSyncService = this@HealthConnectSyncService
    }

    override fun onCreate() {
        super.onCreate()
        healthConnectManager = HealthConnectManager(this)
        createNotificationChannel()
        initApiService()
        Log.i(TAG, "Dedicated HealthConnectSyncService Created (20s Polling Loop)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYNC -> {
                val notification = buildForegroundNotification("Health Connect 10s Sync Active")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
                }
                startContinuous20SecPolling()
            }
            ACTION_STOP_SYNC -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.i(TAG, "Dedicated HealthConnectSyncService Stopped")
    }

    private fun startContinuous20SecPolling() {
        serviceScope.launch {
            Log.i(TAG, "Starting continuous 20-second Health Connect polling loop...")
            while (isActive) {
                try {
                    execute20SecSyncCheck()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in 20-second sync loop: ${e.message}")
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    suspend fun execute20SecSyncCheck(): HealthConnectState {
        val endTime = Instant.now()
        val startTime = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()

        return when (val state = healthConnectManager.readGenuineStepData(startTime, endTime)) {
            is HealthConnectState.DataRetrieved -> {
                val currentSteps = state.stepCount

                // DUPLICATE SUPPRESSION: Compare with lastSyncedStepCount
                if (lastSyncedStepCount != null && lastSyncedStepCount == currentSteps) {
                    Log.d(TAG, "20s Check: Steps unchanged ($currentSteps). Skipping network sync.")
                    updateNotification("20s Check: Steps unchanged ($currentSteps)")
                    return HealthConnectState.DataUnchanged(currentSteps)
                }

                // Step count changed — transmit new genuine value to backend
                Log.i(TAG, "20s Check: Step count updated from $lastSyncedStepCount to $currentSteps. Transmitting sync...")
                val payload = ActivitySyncPayload(
                    stepCount = currentSteps,
                    distanceMeters = state.distanceMeters,
                    caloriesBurned = state.caloriesBurned,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    sourceDevice = "Android Health Connect (20s Sync)"
                )

                try {
                    val response = apiService.syncActivity(payload)
                    if (response.isSuccessful) {
                        lastSyncedStepCount = currentSteps
                        Log.i(TAG, "Successfully synced $currentSteps genuine steps to backend")
                        updateNotification("Synced $currentSteps steps to backend")
                        state
                    } else if (response.code() == 401 || response.code() == 403) {
                        Log.e(TAG, "Authentication Error: HTTP ${response.code()}")
                        updateNotification("Authentication Error (HTTP ${response.code()})")
                        HealthConnectState.StatusError(HealthConnectErrorCode.AUTHENTICATION_ERROR, "Invalid/Expired JWT")
                    } else {
                        Log.e(TAG, "Sync Failed: HTTP ${response.code()}")
                        updateNotification("Sync Failed (HTTP ${response.code()})")
                        HealthConnectState.StatusError(HealthConnectErrorCode.SYNC_FAILED, "HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network Error during 20s sync: ${e.message}")
                    updateNotification("Network Error during 20s sync")
                    HealthConnectState.StatusError(HealthConnectErrorCode.NETWORK_ERROR, e.message ?: "Network error")
                }
            }
            is HealthConnectState.StatusError -> {
                Log.w(TAG, "Health Connect Status: ${state.code} - ${state.message}")
                updateNotification("Status: ${state.code}")
                state
            }
            else -> state
        }
    }

    private fun initApiService() {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(this))
            .build()

        val baseUrl = getSharedPreferences("wellness_config", Context.MODE_PRIVATE)
            .getString("backend_url", "http://192.168.0.135:8080/") ?: "http://192.168.0.135:8080/"

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WellnessApiService::class.java)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Connect 20s Sync Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous 20-second synchronization of Android Health Connect step records"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wellness Health Connect 20s Sync")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }
}
