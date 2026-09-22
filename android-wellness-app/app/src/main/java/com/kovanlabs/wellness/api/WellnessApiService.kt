package com.kovanlabs.wellness.api

import com.kovanlabs.wellness.model.ActivitySyncPayload
import com.kovanlabs.wellness.model.ActivitySyncResponse
import com.kovanlabs.wellness.model.ChallengeResponse
import com.kovanlabs.wellness.model.LoginRequest
import com.kovanlabs.wellness.model.LoginResponse
import com.kovanlabs.wellness.model.RegisterRequest
import com.kovanlabs.wellness.model.StepSyncRequestDto
import com.kovanlabs.wellness.model.StepSyncResponseDto
import com.kovanlabs.wellness.model.TeamLeaderboardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WellnessApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @POST("api/activities/sync")
    suspend fun syncActivity(
        @Body payload: ActivitySyncPayload
    ): Response<ActivitySyncResponse>

    @POST("api/steps/sync")
    suspend fun syncSteps(
        @Body request: StepSyncRequestDto
    ): Response<StepSyncResponseDto>

    @GET("api/teams/1/leaderboard")
    suspend fun getLeaderboard(): Response<TeamLeaderboardResponse>

    @GET("api/challenges")
    suspend fun getChallenges(): Response<List<ChallengeResponse>>
}
