package com.kovanlabs.wellness.api

import com.kovanlabs.wellness.model.*
import retrofit2.Response
import retrofit2.http.*

interface WellnessApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @GET("api/users/me")
    suspend fun getUserProfile(): Response<UserProfileDto>

    @PUT("api/users/me")
    suspend fun updateUserProfile(
        @Body request: UpdateProfileRequestDto
    ): Response<UserProfileDto>

    @GET("api/steps/today")
    suspend fun getTodaySteps(): Response<StepSyncResponseDto>

    @POST("api/steps/sync")
    suspend fun syncSteps(
        @Body request: StepSyncRequestDto
    ): Response<StepSyncResponseDto>

    @POST("api/activities/sync")
    suspend fun syncActivity(
        @Body payload: ActivitySyncPayload
    ): Response<ActivitySyncResponse>

    @GET("api/activities/trends")
    suspend fun getActivityTrends(): Response<ActivityTrendDto>

    @GET("api/challenges")
    suspend fun getChallenges(): Response<List<ChallengeResponse>>

    @POST("api/challenges/{id}/join")
    suspend fun joinChallenge(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/teams/1/leaderboard")
    suspend fun getLeaderboard(): Response<TeamLeaderboardResponse>

    @GET("api/exercises/my")
    suspend fun getMyExercises(): Response<List<ExerciseResponseDto>>

    @POST("api/exercises")
    suspend fun logExercise(
        @Body request: ExerciseLogRequestDto
    ): Response<ExerciseResponseDto>

    @POST("api/ai/chat")
    suspend fun chatAi(
        @Body request: AIChatRequestDto
    ): Response<AIChatResponseDto>
}
