package com.example.data.network

import retrofit2.Response
import retrofit2.http.*

interface VoltEliteApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<BaseApiResponse<TokenResponseDto>>

    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("radius") radiusKm: Double = 50.0,
        @Query("operator") operator: String? = null
    ): Response<BaseApiResponse<List<StationDto>>>

    @GET("api/stations/{station_id}")
    suspend fun getStationDetails(
        @Path("station_id") stationId: String
    ): Response<BaseApiResponse<StationDto>>

    @POST("api/qr/verify")
    suspend fun verifyQR(
        @Body body: QRVerifyRequestDto
    ): Response<BaseApiResponse<QRVerifyResponseDto>>

    @POST("api/charging/start")
    suspend fun startCharging(
        @Body body: StartChargingRequestDto
    ): Response<BaseApiResponse<ChargingSessionDto>>

    @GET("api/charging/active")
    suspend fun getActiveSession(): Response<BaseApiResponse<ChargingSessionDto?>>

    @GET("api/charging/{session_id}")
    suspend fun getChargingSession(
        @Path("session_id") sessionId: String
    ): Response<BaseApiResponse<ChargingSessionDto>>

    @POST("api/charging/{session_id}/stop")
    suspend fun stopCharging(
        @Path("session_id") sessionId: String
    ): Response<BaseApiResponse<Map<String, Any?>>>

    @GET("api/history/charging")
    suspend fun getChargingHistory(): Response<BaseApiResponse<List<HistorySessionDto>>>

    @GET("api/analytics/user")
    suspend fun getUserAnalytics(): Response<BaseApiResponse<Map<String, Any?>>>
}
