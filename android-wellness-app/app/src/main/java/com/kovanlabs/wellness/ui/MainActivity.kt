package com.kovanlabs.wellness.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import com.kovanlabs.wellness.model.StepSyncRequestDto
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

class MainActivity : AppCompatActivity() {

    companion object {
        // Fixed production backend URL deployed on Render
        private const val BASE_URL = "https://ai-wellness-jt1d.onrender.com/"
        private const val TAG = "MainActivity"
    }

    private lateinit var healthConnectManager: HealthConnectManager

    // UI View Containers
    private lateinit var loginContainer: LinearLayout
    private lateinit var dashboardContainer: LinearLayout

    // Login Form Elements
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    // Dashboard Elements
    private lateinit var userEmailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var stepCountTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var permissionButton: Button

    private var autoSyncJob: Job? = null

    // Health Connect Permission Contract
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

        // Bind Views
        loginContainer = findViewById(R.id.loginContainer)
        dashboardContainer = findViewById(R.id.dashboardContainer)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        userEmailTextView = findViewById(R.id.userEmailTextView)
        logoutButton = findViewById(R.id.logoutButton)
        stepCountTextView = findViewById(R.id.stepCountTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        statusTextView = findViewById(R.id.statusTextView)
        permissionButton = findViewById(R.id.permissionButton)

        // Event Listeners
        loginButton.setOnClickListener { handleLogin() }
        logoutButton.setOnClickListener { handleLogout() }
        permissionButton.setOnClickListener { requestHealthConnectPermissions() }
        permissionButton.setOnLongClickListener {
            openHealthConnectSettings()
            true
        }

        // Check authentication state
        checkAuthSession()
    }

    override fun onResume() {
        super.onResume()
        if (isUserLoggedIn()) {
            checkHealthConnectStatus()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
        return !prefs.getString("jwt_token", null).isNull_or_blank()
    }

    private fun checkAuthSession() {
        if (isUserLoggedIn()) {
            val savedEmail = getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
                .getString("saved_email", "Logged In User") ?: "Logged In User"
            userEmailTextView.text = "Logged in: $savedEmail"

            loginContainer.visibility = View.GONE
            dashboardContainer.visibility = View.VISIBLE

            checkHealthConnectStatus()
        } else {
            loginContainer.visibility = View.VISIBLE
            dashboardContainer.visibility = View.GONE
        }
    }

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false
        loginButton.text = "Authenticating..."

        lifecycleScope.launch {
            try {
                val api = getApiService()
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("jwt_token", token)
                        .putString("saved_email", email)
                        .apply()

                    Toast.makeText(this@MainActivity, "Welcome back, $email!", Toast.LENGTH_SHORT).show()
                    checkAuthSession()
                } else {
                    Toast.makeText(this@MainActivity, "Login failed (${response.code()}): Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loginButton.isEnabled = true
                loginButton.text = "SIGN IN"
            }
        }
    }

    private fun handleLogout() {
        autoSyncJob?.cancel()
        getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
            .edit()
            .remove("jwt_token")
            .remove("saved_email")
            .apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        checkAuthSession()
    }

    private fun checkHealthConnectStatus() {
        lifecycleScope.launch {
            when {
                !healthConnectManager.isHealthConnectAvailable() -> {
                    permissionButton.visibility = View.GONE
                    updateUiState(HealthConnectState.StatusError(
                        HealthConnectErrorCode.HEALTH_CONNECT_UNAVAILABLE,
                        "Health Connect SDK not supported on this device"
                    ))
                }
                !healthConnectManager.hasPermissionsGranted() -> {
                    permissionButton.visibility = View.VISIBLE
                    permissionButton.isEnabled = true
                    updateUiState(HealthConnectState.StatusError(
                        HealthConnectErrorCode.PERMISSION_REQUIRED,
                        "Health Connect steps permission required"
                    ))
                }
                else -> {
                    permissionButton.visibility = View.GONE
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

    private fun autoStart10SecSyncService() {
        if (autoSyncJob?.isActive == true) return
        autoSyncJob = lifecycleScope.launch {
            statusTextView.text = "Status: Cloud Auto-Sync Active (Every 10s)"

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
                        val stepDto = StepSyncRequestDto(
                            steps = freshState.stepCount.toLong(),
                            date = LocalDate.now().toString(),
                            sourceDevice = "Android Health Connect (Auto 10s)"
                        )
                        getApiService().syncSteps(stepDto)

                        // 2. Send activity sync to /api/activities/sync
                        val payload = ActivitySyncPayload(
                            stepCount = freshState.stepCount,
                            distanceMeters = freshState.distanceMeters,
                            caloriesBurned = freshState.caloriesBurned,
                            startTime = freshState.startTime,
                            endTime = freshState.endTime,
                            sourceDevice = "Android Health Connect (Auto 10s)"
                        )
                        getApiService().syncActivity(payload)
                        statusTextView.text = "Status: Live Cloud Sync Active (${freshState.stepCount} steps)"
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto-sync network error: ${e.message}")
                        statusTextView.text = "Status: Retrying cloud connection..."
                    }
                }
                delay(10000L)
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

    private fun updateUiState(state: HealthConnectState) {
        when (state) {
            is HealthConnectState.PermissionGranted -> {
                statusTextView.text = "Status: Health Connect Granted"
                permissionButton.visibility = View.GONE
            }
            is HealthConnectState.DataRetrieved -> {
                stepCountTextView.text = "%,d".format(state.stepCount)
                distanceTextView.text = "%.2f km".format(state.distanceMeters / 1000.0)
                caloriesTextView.text = "%.0f kcal".format(state.caloriesBurned)
                statusTextView.text = "Status: Active Sensor Reading (${state.stepCount} steps)"
                permissionButton.visibility = View.GONE
            }
            is HealthConnectState.DataUnchanged -> {
                stepCountTextView.text = "%,d".format(state.stepCount)
                statusTextView.text = "Status: Steps Up-To-Date (${state.stepCount})"
            }
            is HealthConnectState.StatusError -> {
                statusTextView.text = "Status: ${state.message}"
            }
            else -> {}
        }
    }

    private fun getApiService(): WellnessApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(this))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WellnessApiService::class.java)
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
