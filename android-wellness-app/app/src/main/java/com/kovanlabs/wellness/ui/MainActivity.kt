package com.kovanlabs.wellness.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.kovanlabs.wellness.R
import com.kovanlabs.wellness.api.AuthInterceptor
import com.kovanlabs.wellness.api.WellnessApiService
import com.kovanlabs.wellness.health.HealthConnectManager
import com.kovanlabs.wellness.model.ActivitySyncPayload
import com.kovanlabs.wellness.model.HealthConnectErrorCode
import com.kovanlabs.wellness.model.HealthConnectState
import com.kovanlabs.wellness.model.LoginRequest
import com.kovanlabs.wellness.service.HealthConnectSyncService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var statusTextView: TextView
    private lateinit var stepCountTextView: TextView
    private lateinit var startSyncButton: Button
    private lateinit var stopSyncButton: Button
    private lateinit var permissionButton: Button
    private lateinit var loginButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var backendUrlEditText: EditText
    private lateinit var saveUrlButton: Button

    // Health Connect Permission Request Contract
    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    private val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { _ ->
        lifecycleScope.launch {
            checkHealthConnectStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        healthConnectManager = HealthConnectManager(this)

        statusTextView = findViewById(R.id.statusTextView)
        stepCountTextView = findViewById(R.id.stepCountTextView)
        startSyncButton = findViewById(R.id.syncButton)
        stopSyncButton = findViewById(R.id.stopSyncButton)
        permissionButton = findViewById(R.id.permissionButton)
        loginButton = findViewById(R.id.loginButton)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        backendUrlEditText = findViewById(R.id.backendUrlEditText)
        saveUrlButton = findViewById(R.id.saveUrlButton)

        // Load saved server URL or default to cloud URL
        val savedUrl = getSharedPreferences("wellness_config", MODE_PRIVATE)
            .getString("backend_url", "https://ai-wellness-jtid.onrender.com/") ?: "https://ai-wellness-jtid.onrender.com/"
        backendUrlEditText.setText(savedUrl)

        saveUrlButton.setOnClickListener {
            val inputUrl = backendUrlEditText.text.toString().trim()
            val finalUrl = if (inputUrl.endsWith("/")) inputUrl else "$inputUrl/"
            getSharedPreferences("wellness_config", MODE_PRIVATE)
                .edit()
                .putString("backend_url", finalUrl)
                .apply()
            Toast.makeText(this, "Saved Server URL: $finalUrl", Toast.LENGTH_SHORT).show()
        }

        loginButton.setOnClickListener { handleLogin() }
        permissionButton.setOnClickListener { requestHealthConnectPermissions() }
        permissionButton.setOnLongClickListener {
            openHealthConnectSettings()
            true
        }
        startSyncButton.setOnClickListener { startDedicated20SecSyncService() }
        stopSyncButton.setOnClickListener { stopDedicated20SecSyncService() }

        checkHealthConnectStatus()
    }

    override fun onResume() {
        super.onResume()
        checkHealthConnectStatus()
    }

    private fun checkHealthConnectStatus() {
        lifecycleScope.launch {
            when {
                !healthConnectManager.isHealthConnectAvailable() -> {
                    updateUiState(HealthConnectState.StatusError(
                        HealthConnectErrorCode.HEALTH_CONNECT_UNAVAILABLE,
                        "Health Connect SDK not supported/installed"
                    ))
                }
                !healthConnectManager.hasPermissionsGranted() -> {
                    updateUiState(HealthConnectState.StatusError(
                        HealthConnectErrorCode.PERMISSION_REQUIRED,
                        "Health Connect READ_STEPS permission required"
                    ))
                }
                else -> {
                    val endTime = Instant.now()
                    val startTime = LocalDate.now(ZoneId.systemDefault())
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                    val dataState = healthConnectManager.readGenuineStepData(startTime, endTime)
                    updateUiState(dataState)
                    autoStart10SecSyncService()
                }
            }
        }
    }

    private fun requestHealthConnectPermissions() {
        try {
            requestPermissions.launch(healthConnectManager.REQUIRED_PERMISSIONS)
        } catch (_: Exception) {
            openHealthConnectSettings()
        }
    }

    private fun openHealthConnectSettings() {
        val intents = listOf(
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS),
            Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"),
            Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        Toast.makeText(this, "Please grant Health Connect permissions in Settings", Toast.LENGTH_LONG).show()
    }

    private var autoSyncJob: Job? = null

    private fun autoStart10SecSyncService() {
        if (autoSyncJob?.isActive == true) return
        autoSyncJob = lifecycleScope.launch {
            statusTextView.text = "Status: 10-Second Auto-Sync Active"
            startSyncButton.isEnabled = false
            stopSyncButton.isEnabled = true

            while (isActive) {
                val endTime = Instant.now()
                val startTime = LocalDate.now(ZoneId.systemDefault())
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()

                val freshState = healthConnectManager.readGenuineStepData(startTime, endTime)
                updateUiState(freshState)

                if (freshState is HealthConnectState.DataRetrieved) {
                    try {
                        // 1. Send idempotent step sync to /api/steps/sync
                        val stepDto = com.kovanlabs.wellness.model.StepSyncRequestDto(
                            steps = freshState.stepCount.toLong(),
                            date = LocalDate.now().toString(),
                            sourceDevice = "Android Health Connect (Auto 10s)"
                        )
                        getApiService().syncSteps(stepDto)

                        // 2. Also send activity sync to /api/activities/sync
                        val payload = ActivitySyncPayload(
                            stepCount = freshState.stepCount,
                            distanceMeters = freshState.distanceMeters,
                            caloriesBurned = freshState.caloriesBurned,
                            startTime = freshState.startTime,
                            endTime = freshState.endTime,
                            sourceDevice = "Android Health Connect (Auto 10s)"
                        )
                        getApiService().syncActivity(payload)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Auto-sync network error: ${e.message}")
                    }
                }
                delay(10000L)
            }
        }
    }

    private fun startDedicated20SecSyncService() {
        autoStart10SecSyncService()
    }

    private fun stopDedicated20SecSyncService() {
        autoSyncJob?.cancel()
        statusTextView.text = "Status: Auto-Sync Stopped"
        startSyncButton.isEnabled = true
        stopSyncButton.isEnabled = false
    }

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val api = getApiService()
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("jwt_token", token)
                        .apply()

                    Toast.makeText(this@MainActivity, "Authenticated as $email", Toast.LENGTH_SHORT).show()
                    checkHealthConnectStatus()
                } else {
                    Toast.makeText(this@MainActivity, "Login failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUiState(state: HealthConnectState) {
        when (state) {
            is HealthConnectState.PermissionGranted -> {
                statusTextView.text = "Status: Health Connect Permissions Granted"
                permissionButton.isEnabled = false
                startSyncButton.isEnabled = true
            }
            is HealthConnectState.DataRetrieved -> {
                statusTextView.text = "Status: Genuine StepsRecord Active (${state.stepCount} steps)"
                stepCountTextView.text = "${state.stepCount} steps"
                permissionButton.isEnabled = false
                startSyncButton.isEnabled = true
            }
            is HealthConnectState.DataUnchanged -> {
                statusTextView.text = "Status: 20s Check - Steps Unchanged (${state.stepCount})"
            }
            is HealthConnectState.StatusError -> {
                statusTextView.text = "Status: ${state.code} - ${state.message}"
                when (state.code) {
                    HealthConnectErrorCode.HEALTH_CONNECT_UNAVAILABLE -> {
                        permissionButton.isEnabled = false
                        startSyncButton.isEnabled = false
                    }
                    HealthConnectErrorCode.PERMISSION_REQUIRED -> {
                        permissionButton.isEnabled = true
                        startSyncButton.isEnabled = false
                    }
                    HealthConnectErrorCode.NO_DATA_AVAILABLE -> {
                        stepCountTextView.text = "NO DATA AVAILABLE"
                    }
                    else -> {}
                }
            }
            else -> {}
        }
    }

    private fun getApiService(): WellnessApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(this))
            .build()

        val baseUrl = getSharedPreferences("wellness_config", MODE_PRIVATE)
            .getString("backend_url", "http://10.0.2.2:8080/") ?: "http://10.0.2.2:8080/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WellnessApiService::class.java)
    }
}
