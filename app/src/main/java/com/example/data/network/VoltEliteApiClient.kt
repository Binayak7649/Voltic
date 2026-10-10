package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object VoltEliteApiClient {

    // Default Android Emulator loopback to host machine backend (http://10.0.2.2:8001/)
    // Can be overridden at runtime or via environment
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8001/"
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            field = formatted
            synchronized(this) {
                retrofitInstance = null
                apiServiceInstance = null
            }
        }

    @Volatile
    var authToken: String? = null

    @Volatile
    private var retrofitInstance: Retrofit? = null
    @Volatile
    private var apiServiceInstance: VoltEliteApiService? = null

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC  // Lightweight header-only logging for optimal performance
        }

        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                authToken?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    fun getService(): VoltEliteApiService {
        val current = apiServiceInstance
        if (current != null) return current

        return synchronized(this) {
            apiServiceInstance ?: run {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                retrofitInstance = retrofit
                val service = retrofit.create(VoltEliteApiService::class.java)
                apiServiceInstance = service
                service
            }
        }
    }

    fun connectChargingWebSocket(
        sessionId: String,
        onUpdate: (percent: Float, energyKwh: Double, cost: Double, status: String) -> Unit
    ): okhttp3.WebSocket {
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "api/charging/ws/$sessionId"
        val request = okhttp3.Request.Builder().url(wsUrl).build()
        return okHttpClient.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = org.json.JSONObject(text)
                    val battery = json.optDouble("battery_percentage", 0.0).toFloat()
                    val energy = json.optDouble("energy_kwh", 0.0)
                    val cost = json.optDouble("estimated_cost", 0.0)
                    val status = json.optString("status", "CHARGING")
                    onUpdate(battery, energy, cost, status)
                } catch (e: Exception) {
                    // Fallback to local
                }
            }
        })
    }
}
