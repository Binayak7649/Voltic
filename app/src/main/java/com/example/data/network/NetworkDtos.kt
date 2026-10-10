package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BaseApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error_code") val errorCode: String? = null
)

@JsonClass(generateAdapter = true)
data class StationDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "operator") val operator: String,
    @Json(name = "address") val address: String,
    @Json(name = "city") val city: String,
    @Json(name = "state") val state: String? = null,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "distance_km") val distanceKm: Double? = 0.0,
    @Json(name = "eta_minutes") val etaMinutes: Int? = 5,
    @Json(name = "rating") val rating: Double? = 4.5,
    @Json(name = "reviews_count") val reviewsCount: Int? = 100,
    @Json(name = "price_per_kwh") val pricePerKwh: Double? = 18.5,
    @Json(name = "total_available") val totalAvailable: Int? = 1,
    @Json(name = "total_ports") val totalPorts: Int? = 2,
    @Json(name = "max_power_kw") val maxPowerKw: Int? = 60,
    @Json(name = "open_hours") val openHours: String? = "24/7 Open",
    @Json(name = "amenities") val amenities: List<String>? = emptyList(),
    @Json(name = "is_car_compatible") val isCarCompatible: Boolean? = true,
    @Json(name = "chargers") val chargers: List<ChargerDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChargerDto(
    @Json(name = "id") val id: String,
    @Json(name = "charger_code") val chargerCode: String,
    @Json(name = "connector_type") val connectorType: String,
    @Json(name = "power_kw") val powerKw: Int,
    @Json(name = "charging_speed") val chargingSpeed: String? = "DC Fast",
    @Json(name = "price_per_kwh") val pricePerKwh: Double,
    @Json(name = "status") val status: String,
    @Json(name = "qr_code") val qrCode: String,
    @Json(name = "supports_remote_start") val supportsRemoteStart: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class QRVerifyRequestDto(
    @Json(name = "qr_payload") val qrPayload: String,
    @Json(name = "vehicle_make_model") val vehicleMakeModel: String? = "Tata Nexon EV",
    @Json(name = "vehicle_connector") val vehicleConnector: String? = "CCS 2"
)

@JsonClass(generateAdapter = true)
data class VehicleCompatibilityDto(
    @Json(name = "is_compatible") val isCompatible: Boolean,
    @Json(name = "user_vehicle_make_model") val userVehicleMakeModel: String,
    @Json(name = "user_vehicle_connector") val userVehicleConnector: String,
    @Json(name = "charger_connector") val chargerConnector: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class QRVerifyResponseDto(
    @Json(name = "is_valid") val isValid: Boolean,
    @Json(name = "station_id") val stationId: String,
    @Json(name = "station_name") val stationName: String,
    @Json(name = "operator") val operator: String,
    @Json(name = "evse_id") val evseId: String,
    @Json(name = "charger_id") val chargerId: String,
    @Json(name = "connector_type") val connectorType: String,
    @Json(name = "power_kw") val powerKw: Int,
    @Json(name = "tariff_per_kwh") val tariffPerKwh: Double,
    @Json(name = "location") val location: String,
    @Json(name = "status") val status: String,
    @Json(name = "supports_remote_start") val supportsRemoteStart: Boolean,
    @Json(name = "compatibility") val compatibility: VehicleCompatibilityDto,
    @Json(name = "estimated_full_cost") val estimatedFullCost: Int,
    @Json(name = "estimated_duration_min") val estimatedDurationMin: Int
)

@JsonClass(generateAdapter = true)
data class StartChargingRequestDto(
    @Json(name = "charger_id") val chargerId: String,
    @Json(name = "start_percentage") val startPercentage: Float = 20f,
    @Json(name = "target_percentage") val targetPercentage: Float = 85f,
    @Json(name = "payment_method") val paymentMethod: String = "UPI",
    @Json(name = "is_demo") val isDemo: Boolean = false,
    @Json(name = "idempotency_key") val idempotencyKey: String? = null
)

@JsonClass(generateAdapter = true)
data class ChargingSessionDto(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "station_name") val stationName: String? = null,
    @Json(name = "evse_id") val evseId: String,
    @Json(name = "status") val status: String,
    @Json(name = "current_percentage") val currentPercentage: Float,
    @Json(name = "target_percentage") val targetPercentage: Float? = 85f,
    @Json(name = "energy_consumed_kwh") val energyConsumedKwh: Double,
    @Json(name = "charging_duration") val chargingDuration: Int? = 0,
    @Json(name = "power_kw") val powerKw: Int? = 60,
    @Json(name = "tariff_per_kwh") val tariffPerKwh: Double? = 18.5,
    @Json(name = "estimated_cost") val estimatedCost: Double,
    @Json(name = "final_cost") val finalCost: Double? = 0.0,
    @Json(name = "txn_id") val txnId: String? = null,
    @Json(name = "payment_method") val paymentMethod: String? = "UPI",
    @Json(name = "is_demo") val isDemo: Boolean? = false,
    @Json(name = "provider_name") val providerName: String? = null
)

@JsonClass(generateAdapter = true)
data class ProviderStatusDto(
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "operator_name") val operatorName: String,
    @Json(name = "status") val status: String,
    @Json(name = "details") val details: String
)

@JsonClass(generateAdapter = true)
data class HistorySessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "station_name") val stationName: String,
    @Json(name = "operator") val operator: String,
    @Json(name = "evse_id") val evseId: String,
    @Json(name = "connector_type") val connectorType: String,
    @Json(name = "power_kw") val powerKw: Int,
    @Json(name = "energy_consumed_kwh") val energyConsumedKwh: Double,
    @Json(name = "charging_duration_minutes") val chargingDurationMinutes: Int,
    @Json(name = "total_cost") val totalCost: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "status") val status: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "txn_id") val txnId: String?,
    @Json(name = "invoice_number") val invoiceNumber: String?
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email_or_phone") val emailOrPhone: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "user") val user: Map<String, Any?>
)
