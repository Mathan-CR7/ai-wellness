package com.kovanlabs.wellness.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kovanlabs.wellness.R
import com.kovanlabs.wellness.api.AuthInterceptor
import com.kovanlabs.wellness.api.WellnessApiService
import com.kovanlabs.wellness.health.HealthConnectManager
import com.kovanlabs.wellness.model.*
import com.kovanlabs.wellness.websocket.StompWebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

data class ScheduledWorkoutMobileTask(
    val id: String,
    val exerciseType: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val scheduledDateTimeMillis: Long,
    val notes: String? = null,
    var status: String = "SCHEDULED", // SCHEDULED, RUNNING, PAUSED, COMPLETED
    var secondsLeft: Int = durationMinutes * 60
)

class MainActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "https://ai-wellness-jt1d.onrender.com/"
        private const val TAG = "MainActivity"
        private const val SCHEDULED_WORKOUTS_PREFS = "scheduled_workouts_prefs"
        private const val KEY_TASKS_JSON = "tasks_json"
    }

    private lateinit var healthConnectManager: HealthConnectManager
    private var stompClient: StompWebSocketClient? = null

    // UI Containers
    private lateinit var loginContainer: View
    private lateinit var dashboardContainer: View

    // Tab ScrollViews / Layouts
    private lateinit var tabHome: View
    private lateinit var tabAiCoach: View
    private lateinit var tabChallenges: View
    private lateinit var tabLeaderboard: View
    private lateinit var tabProfile: View

    // Bottom Navigation TextViews
    private lateinit var navHome: TextView
    private lateinit var navAiCoach: TextView
    private lateinit var navChallenges: TextView
    private lateinit var navLeaderboard: TextView
    private lateinit var navProfile: TextView

    // Auth Elements
    private lateinit var authTitleTextView: TextView
    private lateinit var fullNameLabelTextView: TextView
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var toggleRegisterTextView: TextView

    // Dashboard Header & Home Elements
    private lateinit var userEmailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var stepCountTextView: TextView
    private lateinit var stepGoalTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var pulseStatusTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var permissionButton: Button

    // AI Inactivity Suggestion Elements
    private lateinit var aiSuggestionCard: View
    private lateinit var aiSuggestionTextView: TextView
    private lateinit var aiSuggestionTimeTextView: TextView

    // AI Coach Chat Elements
    private lateinit var aiChatLogTextView: TextView
    private lateinit var aiChatInputEditText: EditText
    private lateinit var aiChatSendButton: Button

    // Challenge Elements
    private lateinit var challengeTitleTextView: TextView
    private lateinit var challengeProgressBar: ProgressBar
    private lateinit var challengeStatusTextView: TextView

    // Leaderboard Container
    private lateinit var leaderboardContainer: LinearLayout

    // Profile, Workouts & Scheduler Elements
    private lateinit var exerciseTypeEditText: EditText
    private lateinit var exerciseDurationEditText: EditText
    private lateinit var logExerciseButton: Button
    private lateinit var scheduleWorkoutButton: Button
    private lateinit var scheduledWorkoutsContainer: LinearLayout
    private lateinit var completedExercisesContainer: LinearLayout

    private var isRegisterMode = false
    private var autoSyncJob: Job? = null
    private var scheduledTasksTickerJob: Job? = null
    private var activeWorkoutTimerJobs = mutableMapOf<String, Job>()
    private var currentStepCount = 0
    private var currentGoal = 10000

    private var scheduledTasksList = mutableListOf<ScheduledWorkoutMobileTask>()
    private val gson = Gson()

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

        // Bind Containers
        loginContainer = findViewById(R.id.loginContainer)
        dashboardContainer = findViewById(R.id.dashboardContainer)

        tabHome = findViewById(R.id.tabHome)
        tabAiCoach = findViewById(R.id.tabAiCoach)
        tabChallenges = findViewById(R.id.tabChallenges)
        tabLeaderboard = findViewById(R.id.tabLeaderboard)
        tabProfile = findViewById(R.id.tabProfile)

        navHome = findViewById(R.id.navHome)
        navAiCoach = findViewById(R.id.navAiCoach)
        navChallenges = findViewById(R.id.navChallenges)
        navLeaderboard = findViewById(R.id.navLeaderboard)
        navProfile = findViewById(R.id.navProfile)

        // Bind Auth
        authTitleTextView = findViewById(R.id.authTitleTextView)
        fullNameLabelTextView = findViewById(R.id.fullNameLabelTextView)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        toggleRegisterTextView = findViewById(R.id.toggleRegisterTextView)

        // Bind Dashboard
        userEmailTextView = findViewById(R.id.userEmailTextView)
        logoutButton = findViewById(R.id.logoutButton)
        stepCountTextView = findViewById(R.id.stepCountTextView)
        stepGoalTextView = findViewById(R.id.stepGoalTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        pulseStatusTextView = findViewById(R.id.pulseStatusTextView)
        statusTextView = findViewById(R.id.statusTextView)
        permissionButton = findViewById(R.id.permissionButton)

        aiSuggestionCard = findViewById(R.id.aiSuggestionCard)
        aiSuggestionTextView = findViewById(R.id.aiSuggestionTextView)
        aiSuggestionTimeTextView = findViewById(R.id.aiSuggestionTimeTextView)

        // Bind AI Chat
        aiChatLogTextView = findViewById(R.id.aiChatLogTextView)
        aiChatInputEditText = findViewById(R.id.aiChatInputEditText)
        aiChatSendButton = findViewById(R.id.aiChatSendButton)

        // Bind Challenges
        challengeTitleTextView = findViewById(R.id.challengeTitleTextView)
        challengeProgressBar = findViewById(R.id.challengeProgressBar)
        challengeStatusTextView = findViewById(R.id.challengeStatusTextView)

        // Bind Leaderboard
        leaderboardContainer = findViewById(R.id.leaderboardContainer)

        // Bind Profile & Exercises
        exerciseTypeEditText = findViewById(R.id.exerciseTypeEditText)
        exerciseDurationEditText = findViewById(R.id.exerciseDurationEditText)
        logExerciseButton = findViewById(R.id.logExerciseButton)
        scheduleWorkoutButton = findViewById(R.id.scheduleWorkoutButton)
        scheduledWorkoutsContainer = findViewById(R.id.scheduledWorkoutsContainer)
        completedExercisesContainer = findViewById(R.id.completedExercisesContainer)

        // Setup Listeners
        loginButton.setOnClickListener {
            if (isRegisterMode) handleRegister() else handleLogin()
        }

        toggleRegisterTextView.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateAuthUiMode()
        }

        logoutButton.setOnClickListener { handleLogout() }
        permissionButton.setOnClickListener { requestHealthConnectPermissions() }

        // Setup Bottom Nav Clicks
        navHome.setOnClickListener { switchTab(0) }
        navAiCoach.setOnClickListener { switchTab(1) }
        navChallenges.setOnClickListener { switchTab(2) }
        navLeaderboard.setOnClickListener { switchTab(3) }
        navProfile.setOnClickListener { switchTab(4) }

        // AI Chat Send Listener
        aiChatSendButton.setOnClickListener { handleSendAiChat() }

        // Workout Log Listener
        logExerciseButton.setOnClickListener { handleLogExercise() }

        // Schedule Workout Dialog Listener
        scheduleWorkoutButton.setOnClickListener { showScheduleWorkoutDialog() }

        // Start Break Listener
        findViewById<Button>(R.id.startBreakButton)?.setOnClickListener {
            showGuidedMovementBreakDialog()
        }

        loadSavedScheduledTasks()
        startScheduledTasksTicker()
        checkAuthSession()
    }

    override fun onResume() {
        super.onResume()
        if (isUserLoggedIn()) {
            checkHealthConnectStatus()
            fetchMyExercises()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient?.disconnect()
        scheduledTasksTickerJob?.cancel()
        activeWorkoutTimerJobs.values.forEach { it.cancel() }
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
        return !prefs.getString("jwt_token", null).isNullOrBlank()
    }

    private fun updateAuthUiMode() {
        if (isRegisterMode) {
            authTitleTextView.text = "Account Registration"
            fullNameLabelTextView.visibility = View.VISIBLE
            fullNameEditText.visibility = View.VISIBLE
            loginButton.text = "CREATE ACCOUNT & SIGN IN"
            toggleRegisterTextView.text = "Already have an account? Sign In"
        } else {
            authTitleTextView.text = "Account Login"
            fullNameLabelTextView.visibility = View.GONE
            fullNameEditText.visibility = View.GONE
            loginButton.text = "SIGN IN"
            toggleRegisterTextView.text = "Don't have an account? Register here"
        }
    }

    private fun switchTab(index: Int) {
        tabHome.visibility = if (index == 0) View.VISIBLE else View.GONE
        tabAiCoach.visibility = if (index == 1) View.VISIBLE else View.GONE
        tabChallenges.visibility = if (index == 2) View.VISIBLE else View.GONE
        tabLeaderboard.visibility = if (index == 3) View.VISIBLE else View.GONE
        tabProfile.visibility = if (index == 4) View.VISIBLE else View.GONE

        navHome.setTextColor(if (index == 0) Color.parseColor("#2E7D32") else Color.parseColor("#777777"))
        navAiCoach.setTextColor(if (index == 1) Color.parseColor("#6A1B9A") else Color.parseColor("#777777"))
        navChallenges.setTextColor(if (index == 2) Color.parseColor("#E65100") else Color.parseColor("#777777"))
        navLeaderboard.setTextColor(if (index == 3) Color.parseColor("#1565C0") else Color.parseColor("#777777"))
        navProfile.setTextColor(if (index == 4) Color.parseColor("#2E7D32") else Color.parseColor("#777777"))
    }

    private fun checkAuthSession() {
        if (isUserLoggedIn()) {
            val savedEmail = getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
                .getString("saved_email", "Logged In User") ?: "Logged In User"
            userEmailTextView.text = "Logged in: $savedEmail"
            logoutButton.visibility = View.VISIBLE

            loginContainer.visibility = View.GONE
            dashboardContainer.visibility = View.VISIBLE

            checkHealthConnectStatus()
            fetchUserProfile()
            fetchLeaderboard()
            fetchChallenges()
            fetchMyExercises()
            initWebSocket()
        } else {
            loginContainer.visibility = View.VISIBLE
            dashboardContainer.visibility = View.GONE
            logoutButton.visibility = View.GONE
        }
    }

    private fun initWebSocket() {
        stompClient = StompWebSocketClient { suggestion ->
            runOnUiThread {
                aiSuggestionTextView.text = suggestion.suggestion
                aiSuggestionTimeTextView.text = "Just now"
                aiSuggestionCard.visibility = View.VISIBLE
            }
        }
        stompClient?.connect()
    }

    private fun handleRegister() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter name, email, and password", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false
        loginButton.text = "Registering Account..."

        lifecycleScope.launch {
            try {
                val api = getApiService()
                val response = api.register(RegisterRequest(fullName, email, password))
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("jwt_token", token)
                        .putString("saved_email", email)
                        .apply()

                    Toast.makeText(this@MainActivity, "Registration Successful! Welcome $fullName", Toast.LENGTH_SHORT).show()
                    checkAuthSession()
                } else {
                    Toast.makeText(this@MainActivity, "Registration failed (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register error: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loginButton.isEnabled = true
                loginButton.text = if (isRegisterMode) "CREATE ACCOUNT & SIGN IN" else "SIGN IN"
            }
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
                    Toast.makeText(this@MainActivity, "Login failed: Invalid credentials", Toast.LENGTH_SHORT).show()
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
        stompClient?.disconnect()
        getSharedPreferences("wellness_auth_prefs", MODE_PRIVATE)
            .edit()
            .remove("jwt_token")
            .remove("saved_email")
            .apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        checkAuthSession()
    }

    private fun fetchUserProfile() {
        lifecycleScope.launch {
            try {
                val response = getApiService().getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    currentGoal = profile.dailyStepGoal ?: 10000
                    stepGoalTextView.text = "/ %,d steps goal".format(currentGoal)
                    userEmailTextView.text = "${profile.fullName ?: profile.email}"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fetch profile error: ${e.message}")
            }
        }
    }

    private fun handleSendAiChat() {
        val userMsg = aiChatInputEditText.text.toString().trim()
        if (userMsg.isBlank()) return

        aiChatLogTextView.append("\n\nYou: $userMsg")
        aiChatInputEditText.setText("")
        aiChatSendButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = getApiService().chatAi(AIChatRequestDto(userMsg))
                if (response.isSuccessful && response.body() != null) {
                    val reply = response.body()!!.message
                    aiChatLogTextView.append("\n\n🤖 AI Coach: $reply")
                } else {
                    aiChatLogTextView.append("\n\n🤖 AI Coach: I am processing your health data...")
                }
            } catch (e: Exception) {
                aiChatLogTextView.append("\n\n🤖 AI Coach: Connection error: ${e.message}")
            } finally {
                aiChatSendButton.isEnabled = true
            }
        }
    }

    private fun handleLogExercise() {
        val type = exerciseTypeEditText.text.toString().trim()
        val durationStr = exerciseDurationEditText.text.toString().trim()

        if (type.isBlank() || durationStr.isBlank()) {
            Toast.makeText(this, "Please enter workout type and duration", Toast.LENGTH_SHORT).show()
            return
        }

        val duration = durationStr.toIntOrNull() ?: 30
        val calories = duration * 5

        logExerciseButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = getApiService().logExercise(ExerciseLogRequestDto(type, duration, calories))
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Workout logged: $type ($duration mins)", Toast.LENGTH_SHORT).show()
                    exerciseTypeEditText.setText("")
                    exerciseDurationEditText.setText("")
                    fetchMyExercises()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error logging workout: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                logExerciseButton.isEnabled = true
            }
        }
    }

    // ---------------------------------------------------------------------------
    // WORKOUT TASK SCHEDULER & LOCKED TIMERS (MOBILE)
    // ---------------------------------------------------------------------------

    private fun loadSavedScheduledTasks() {
        try {
            val prefs = getSharedPreferences(SCHEDULED_WORKOUTS_PREFS, MODE_PRIVATE)
            val json = prefs.getString(KEY_TASKS_JSON, null)
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<ScheduledWorkoutMobileTask>>() {}.type
                val loaded: List<ScheduledWorkoutMobileTask> = gson.fromJson(json, type)
                scheduledTasksList.clear()
                scheduledTasksList.addAll(loaded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved scheduled tasks: ${e.message}")
        }
    }

    private fun saveScheduledTasks() {
        try {
            val json = gson.toJson(scheduledTasksList)
            getSharedPreferences(SCHEDULED_WORKOUTS_PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_TASKS_JSON, json)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving scheduled tasks: ${e.message}")
        }
    }

    private fun startScheduledTasksTicker() {
        scheduledTasksTickerJob?.cancel()
        scheduledTasksTickerJob = lifecycleScope.launch {
            while (isActive) {
                renderScheduledTasksUI()
                delay(1000L)
            }
        }
    }

    private fun showScheduleWorkoutDialog() {
        val context = this
        val cal = Calendar.getInstance()

        var selectedYear = cal.get(Calendar.YEAR)
        var selectedMonth = cal.get(Calendar.MONTH)
        var selectedDay = cal.get(Calendar.DAY_OF_MONTH)
        var selectedHour = cal.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = cal.get(Calendar.MINUTE) + 5

        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }

        val titleTv = TextView(context).apply {
            text = "📅 Schedule Workout with Date & Time"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B5E20"))
            setPadding(0, 0, 0, 16)
        }

        // Exercise Type Spinner
        val typeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("WALKING", "RUNNING", "CYCLING", "SWIMMING", "YOGA", "STRENGTH_TRAINING", "HIIT", "OTHER")
            )
        }

        // Date & Time Buttons
        val dateBtn = Button(context).apply {
            text = "Select Date (%04d-%02d-%02d)".format(selectedYear, selectedMonth + 1, selectedDay)
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setTextColor(Color.parseColor("#1B5E20"))
        }

        val timeBtn = Button(context).apply {
            text = "Select Time (%02d:%02d)".format(selectedHour, selectedMinute)
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setTextColor(Color.parseColor("#1B5E20"))
        }

        dateBtn.setOnClickListener {
            DatePickerDialog(context, { _, y, m, d ->
                selectedYear = y
                selectedMonth = m
                selectedDay = d
                dateBtn.text = "Select Date (%04d-%02d-%02d)".format(y, m + 1, d)
            }, selectedYear, selectedMonth, selectedDay).show()
        }

        timeBtn.setOnClickListener {
            TimePickerDialog(context, { _, h, min ->
                selectedHour = h
                selectedMinute = min
                timeBtn.text = "Select Time (%02d:%02d)".format(h, min)
            }, selectedHour, selectedMinute, true).show()
        }

        val durationEt = EditText(context).apply {
            hint = "Duration in Minutes (e.g. 30)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("30")
            setPadding(12, 12, 12, 12)
        }

        val notesEt = EditText(context).apply {
            hint = "Notes (e.g. Evening outdoor jog)"
            setPadding(12, 12, 12, 12)
        }

        val confirmBtn = Button(context).apply {
            text = "CONFIRM & SCHEDULE TASK"
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }

        dialogView.addView(titleTv)
        dialogView.addView(typeSpinner)
        dialogView.addView(dateBtn)
        dialogView.addView(timeBtn)
        dialogView.addView(durationEt)
        dialogView.addView(notesEt)
        dialogView.addView(confirmBtn)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        confirmBtn.setOnClickListener {
            val durationMins = durationEt.text.toString().toIntOrNull() ?: 30
            val exerciseType = typeSpinner.selectedItem.toString()
            val notesText = notesEt.text.toString().trim()

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val newTask = ScheduledWorkoutMobileTask(
                id = UUID.randomUUID().toString(),
                exerciseType = exerciseType,
                durationMinutes = durationMins,
                caloriesBurned = durationMins * 5,
                scheduledDateTimeMillis = targetCal.timeInMillis,
                notes = if (notesText.isNotBlank()) notesText else null,
                status = "SCHEDULED",
                secondsLeft = durationMins * 60
            )

            scheduledTasksList.add(0, newTask)
            saveScheduledTasks()
            renderScheduledTasksUI()
            dialog.dismiss()

            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            Toast.makeText(context, "📅 Scheduled $exerciseType for ${sdf.format(targetCal.time)}", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun renderScheduledTasksUI() {
        scheduledWorkoutsContainer.removeAllViews()

        if (scheduledTasksList.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No scheduled workouts active."
                textSize = 12f
                setTextColor(Color.GRAY)
                setPadding(16, 16, 16, 16)
            }
            scheduledWorkoutsContainer.addView(emptyTv)
            return
        }

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        for (task in scheduledTasksList) {
            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.WHITE)
                elevation = 3f
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 12)
                layoutParams = params
            }

            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val typeBadge = TextView(this).apply {
                text = "${task.exerciseType} • ${task.status}"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(when (task.status) {
                    "RUNNING" -> Color.parseColor("#2E7D32")
                    "COMPLETED" -> Color.parseColor("#1565C0")
                    else -> Color.parseColor("#E65100")
                })
                setPadding(12, 4, 12, 4)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { weight = 1f }
            }

            val deleteBtn = TextView(this).apply {
                text = "🗑️"
                textSize = 14f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    scheduledTasksList.remove(task)
                    activeWorkoutTimerJobs[task.id]?.cancel()
                    saveScheduledTasks()
                    renderScheduledTasksUI()
                }
            }

            headerLayout.addView(typeBadge)
            headerLayout.addView(deleteBtn)

            val schedTimeTv = TextView(this).apply {
                text = "📅 Scheduled: ${sdf.format(Date(task.scheduledDateTimeMillis))}"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#555555"))
                setPadding(0, 8, 0, 4)
            }

            val detailsTv = TextView(this).apply {
                text = "⏱️ ${task.durationMinutes} Mins  •  🔥 ${task.caloriesBurned} kcal"
                textSize = 12f
                setTextColor(Color.parseColor("#333333"))
            }

            // Monospace Timer Box
            val timerBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#111827"))
                setPadding(16, 12, 16, 12)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 10, 0, 10)
                layoutParams = params
            }

            val m = task.secondsLeft / 60
            val s = task.secondsLeft % 60
            val timerTv = TextView(this).apply {
                text = "%02d:%02d".format(m, s)
                textSize = 28f
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(Color.parseColor("#10B981"))
            }

            val isReady = now >= task.scheduledDateTimeMillis
            val diffMs = task.scheduledDateTimeMillis - now
            val diffMins = Math.max(1, (diffMs / (1000 * 60)).toInt())
            val timeUntilText = if (isReady) "Ready to Start" else "🔒 Starts in ${diffMins}m"

            val timerSubTv = TextView(this).apply {
                text = when (task.status) {
                    "RUNNING" -> "Workout in Progress..."
                    "PAUSED" -> "Timer Paused"
                    "COMPLETED" -> "Workout Completed 🎉"
                    else -> timeUntilText
                }
                textSize = 10f
                setTextColor(Color.parseColor("#9CA3AF"))
            }

            timerBox.addView(timerTv)
            timerBox.addView(timerSubTv)

            // Start / Action Button
            val actionBtnLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            if (task.status == "SCHEDULED") {
                val startBtn = Button(this).apply {
                    text = if (isReady) "▶️ START TIMER NOW" else "🔒 LOCKED • $timeUntilText"
                    setBackgroundColor(if (isReady) Color.parseColor("#2E7D32") else Color.parseColor("#9E9E9E"))
                    setTextColor(Color.WHITE)
                    isEnabled = true
                    setOnClickListener {
                        if (!isReady) {
                            Toast.makeText(this@MainActivity, "🔒 Cannot start yet! Scheduled for ${sdf.format(Date(task.scheduledDateTimeMillis))}", Toast.LENGTH_SHORT).show()
                        } else {
                            startTaskTimer(task)
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                actionBtnLayout.addView(startBtn)
            } else if (task.status == "RUNNING") {
                val pauseBtn = Button(this).apply {
                    text = "⏸️ PAUSE"
                    setBackgroundColor(Color.parseColor("#E65100"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        task.status = "PAUSED"
                        activeWorkoutTimerJobs[task.id]?.cancel()
                        saveScheduledTasks()
                        renderScheduledTasksUI()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val finishBtn = Button(this).apply {
                    text = "✅ FINISH"
                    setBackgroundColor(Color.parseColor("#1565C0"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        finishTaskTimer(task)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                actionBtnLayout.addView(pauseBtn)
                actionBtnLayout.addView(finishBtn)
            } else if (task.status == "PAUSED") {
                val resumeBtn = Button(this).apply {
                    text = "▶️ RESUME"
                    setBackgroundColor(Color.parseColor("#2E7D32"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        startTaskTimer(task)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val finishBtn = Button(this).apply {
                    text = "✅ FINISH"
                    setBackgroundColor(Color.parseColor("#1565C0"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        finishTaskTimer(task)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                actionBtnLayout.addView(resumeBtn)
                actionBtnLayout.addView(finishBtn)
            } else if (task.status == "COMPLETED") {
                val completedTv = TextView(this).apply {
                    text = "✅ Completed & Logged to DB"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#2E7D32"))
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 6)
                }
                actionBtnLayout.addView(completedTv)
            }

            cardLayout.addView(headerLayout)
            cardLayout.addView(schedTimeTv)
            cardLayout.addView(detailsTv)
            cardLayout.addView(timerBox)
            cardLayout.addView(actionBtnLayout)

            scheduledWorkoutsContainer.addView(cardLayout)
        }
    }

    private fun startTaskTimer(task: ScheduledWorkoutMobileTask) {
        task.status = "RUNNING"
        saveScheduledTasks()
        renderScheduledTasksUI()

        activeWorkoutTimerJobs[task.id]?.cancel()
        val job = lifecycleScope.launch {
            while (isActive && task.status == "RUNNING") {
                if (task.secondsLeft > 1) {
                    delay(1000L)
                    task.secondsLeft--
                } else {
                    finishTaskTimer(task)
                    break
                }
            }
        }
        activeWorkoutTimerJobs[task.id] = job
    }

    private fun finishTaskTimer(task: ScheduledWorkoutMobileTask) {
        activeWorkoutTimerJobs[task.id]?.cancel()
        task.status = "COMPLETED"
        task.secondsLeft = 0
        saveScheduledTasks()
        renderScheduledTasksUI()

        Toast.makeText(this, "🎉 Scheduled Workout Completed! Logging to database...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            try {
                getApiService().logExercise(
                    ExerciseLogRequestDto(
                        exerciseType = task.exerciseType,
                        durationMinutes = task.durationMinutes,
                        caloriesBurned = task.caloriesBurned,
                        notes = "[Scheduled Mobile Workout Completed] ${task.notes ?: ""}".trim()
                    )
                )
                fetchMyExercises()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log completed workout: ${e.message}")
            }
        }
    }

    private fun fetchMyExercises() {
        lifecycleScope.launch {
            try {
                val response = getApiService().getMyExercises()
                if (response.isSuccessful && response.body() != null) {
                    renderCompletedExercisesUI(response.body()!!)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fetch exercises error: ${e.message}")
            }
        }
    }

    private fun renderCompletedExercisesUI(exercises: List<ExerciseResponseDto>) {
        completedExercisesContainer.removeAllViews()

        if (exercises.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No completed exercise sessions in history yet."
                textSize = 12f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 8)
            }
            completedExercisesContainer.addView(emptyTv)
            return
        }

        for (ex in exercises) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.parseColor("#F9FAFB"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
            }

            val titleTv = TextView(this).apply {
                text = "🏋️ ${ex.exerciseType}  •  ${ex.durationMinutes} mins  •  ${ex.caloriesBurned} kcal"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
            }

            val dateTv = TextView(this).apply {
                text = "Logged: ${ex.loggedAt ?: "Today"}"
                textSize = 11f
                setTextColor(Color.GRAY)
            }

            rowLayout.addView(titleTv)
            rowLayout.addView(dateTv)

            if (!ex.notes.isNullOrBlank()) {
                val notesTv = TextView(this).apply {
                    text = "\"${ex.notes}\""
                    textSize = 11f
                    setTextColor(Color.parseColor("#4B5563"))
                    setPadding(0, 4, 0, 0)
                }
                rowLayout.addView(notesTv)
            }

            completedExercisesContainer.addView(rowLayout)
        }
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
                    currentStepCount = freshState.stepCount
                    updateChallengeProgress(currentStepCount)
                    updateWellnessPulse(currentStepCount)

                    try {
                        val stepDto = StepSyncRequestDto(
                            steps = freshState.stepCount.toLong(),
                            date = LocalDate.now().toString(),
                            sourceDevice = "Android Health Connect (Auto 10s)"
                        )
                        getApiService().syncSteps(stepDto)

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

                        fetchLeaderboard()
                        fetchChallenges()

                    } catch (e: Exception) {
                        Log.e(TAG, "Auto-sync network error: ${e.message}")
                        statusTextView.text = "Status: Retrying cloud connection..."
                    }
                }
                delay(10000L)
            }
        }
    }

    private fun updateWellnessPulse(steps: Int) {
        val percent = if (currentGoal > 0) (steps.toDouble() / currentGoal * 100).toInt() else 0
        pulseStatusTextView.text = when {
            percent >= 100 -> "Goal Reached! 🎉 Great job completing target."
            percent >= 75 -> "Goal Progressing! 🔥 Keep going strong."
            percent >= 40 -> "Moderately Active 🏃 Steady progress recorded."
            else -> "Low Activity 🧘 Take a short walk & stretch!"
        }
    }

    private fun fetchLeaderboard() {
        lifecycleScope.launch {
            try {
                val response = getApiService().getLeaderboard()
                if (response.isSuccessful && response.body() != null) {
                    val rankings = response.body()!!.rankings
                    renderLeaderboardUI(rankings)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Leaderboard fetch error: ${e.message}")
            }
        }
    }

    private fun renderLeaderboardUI(rankings: List<LeaderboardEntry>) {
        leaderboardContainer.removeAllViews()

        // Squad Header Banner with Invite Code
        val squadHeaderTv = TextView(this).apply {
            text = "⚡ AURA Squad Roster • Invite Code: AURA-SQUAD-2026"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B5E20"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setPadding(16, 12, 16, 12)
        }
        leaderboardContainer.addView(squadHeaderTv)

        if (rankings.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No team activity recorded yet"
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 8)
            }
            leaderboardContainer.addView(emptyTv)
            return
        }

        for (entry in rankings) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val rankTv = TextView(this).apply {
                text = "#${entry.rank}"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(when (entry.rank) {
                    1 -> Color.parseColor("#FF8F00")
                    2 -> Color.parseColor("#757575")
                    3 -> Color.parseColor("#A1887F")
                    else -> Color.parseColor("#333333")
                })
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 16, 0) }
            }

            val nameLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(this).apply {
                text = entry.fullName ?: entry.email ?: "User"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#222222"))
            }

            nameLayout.addView(nameTv)

            // Highlight Squad Creator & Admin
            if (entry.rank == 1) {
                val adminBadgeTv = TextView(this).apply {
                    text = "👑 Squad Admin & Creator"
                    textSize = 10f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#B45309"))
                }
                nameLayout.addView(adminBadgeTv)
            }

            val stepsTv = TextView(this).apply {
                val distKm = entry.totalDistanceMeters / 1000.0
                text = "%,d steps (%.2f km)".format(entry.totalSteps, distKm)
                textSize = 13f
                setTextColor(Color.parseColor("#2E7D32"))
                setTypeface(null, Typeface.BOLD)
            }

            rowLayout.addView(rankTv)
            rowLayout.addView(nameLayout)
            rowLayout.addView(stepsTv)
            leaderboardContainer.addView(rowLayout)
        }
    }

    private fun fetchChallenges() {
        lifecycleScope.launch {
            try {
                val response = getApiService().getChallenges()
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val challenge = response.body()!![0]
                    challengeTitleTextView.text = challenge.title
                    val target = (challenge.targetSteps ?: 10000L).toInt()
                    challengeProgressBar.max = target
                    updateChallengeProgress(currentStepCount, target)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Challenge fetch error: ${e.message}")
            }
        }
    }

    private fun updateChallengeProgress(steps: Int, target: Int = 10000) {
        challengeProgressBar.progress = Math.min(steps, target)
        val percent = if (target > 0) Math.min((steps.toDouble() / target * 100).toInt(), 100) else 0
        challengeStatusTextView.text = "%,d / %,d Steps (%d%%)".format(steps, target, percent)
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
                currentStepCount = state.stepCount
                stepCountTextView.text = "%,d".format(state.stepCount)
                distanceTextView.text = "%.2f km".format(state.distanceMeters / 1000.0)
                caloriesTextView.text = "%.0f kcal".format(state.caloriesBurned)
                statusTextView.text = "Status: Active Sensor Reading (${state.stepCount} steps)"
                permissionButton.visibility = View.GONE
                updateChallengeProgress(currentStepCount)
                updateWellnessPulse(currentStepCount)
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

    private fun showGuidedMovementBreakDialog() {
        val context = this

        data class BreakStage(
            val name: String,
            val durationSec: Int,
            val instruction: String,
            val notice: String
        )

        val stages = listOf(
            BreakStage(
                "Stage 1: 5-Minute Brisk Walk",
                300,
                "🚶 Walk 400+ steps to reactivate lower body circulation & boost metabolism.",
                "🎉 5-Minute Walk Completed! Next Stage: 3-Minute Shoulder Rotations."
            ),
            BreakStage(
                "Stage 2: 3-Minute Shoulder Rotations",
                180,
                "🙆 Roll shoulders backward & forward 15 reps to unlock upper back stiffness.",
                "✅ 3-Minute Shoulder Rotations Completed! Next Stage: 3-Minute Neck Stretches."
            ),
            BreakStage(
                "Stage 3: 3-Minute Neck Release Stretches",
                180,
                "🧘 Gently tilt ear to shoulder holding 30 seconds for each side.",
                "✅ 3-Minute Neck Stretches Completed! Final Stage: 4-Minute Standing Side Torso Stretch."
            ),
            BreakStage(
                "Stage 4: 4-Minute Standing Side Torso Stretch",
                240,
                "🧍 Reach overhead with clasped hands & flex lateral torso for core mobility.",
                "🏆 15-Minute Movement Break Fully Completed! You are refreshed & energized."
            )
        )

        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val titleTv = TextView(context).apply {
            text = "🌿 15-Minute Multi-Stage Movement Break"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B5E20"))
            setPadding(0, 0, 0, 8)
        }

        val stageTitleTv = TextView(context).apply {
            text = stages[0].name
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#6A1B9A"))
            setPadding(0, 0, 0, 8)
        }

        val timerTv = TextView(context).apply {
            text = "05:00"
            textSize = 38f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E65100"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }

        val descTv = TextView(context).apply {
            text = stages[0].instruction
            textSize = 13f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(3f, 1f)
            setPadding(0, 0, 0, 20)
        }

        val closeBtn = Button(context).apply {
            text = "CLOSE / FINISH BREAK"
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }

        dialogView.addView(titleTv)
        dialogView.addView(stageTitleTv)
        dialogView.addView(timerTv)
        dialogView.addView(descTv)
        dialogView.addView(closeBtn)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        var currentStageIdx = 0
        var secondsLeft = stages[0].durationSec

        val timerJob = lifecycleScope.launch {
            while (isActive) {
                if (secondsLeft > 0) {
                    val m = secondsLeft / 60
                    val s = secondsLeft % 60
                    timerTv.text = "%02d:%02d".format(m, s)
                    delay(1000L)
                    secondsLeft--
                } else {
                    val currentStage = stages[currentStageIdx]
                    Toast.makeText(context, currentStage.notice, Toast.LENGTH_LONG).show()

                    if (currentStageIdx < stages.size - 1) {
                        currentStageIdx++
                        val nextStage = stages[currentStageIdx]
                        stageTitleTv.text = nextStage.name
                        descTv.text = nextStage.instruction
                        secondsLeft = nextStage.durationSec
                    } else {
                        timerTv.text = "00:00 (15-Min Break Complete! 🎉)"
                        stageTitleTv.text = "🏆 All 4 Stages Fully Completed!"
                        descTv.text = "Great job completing your 15-minute movement break routine!"
                        break
                    }
                }
            }
        }

        closeBtn.setOnClickListener {
            timerJob.cancel()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            timerJob.cancel()
        }

        dialog.show()
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
}
