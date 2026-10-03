package com.example.model

enum class ChargingNetwork(val displayName: String, val brandColorHex: Long) {
    TATA_POWER("Tata Power", 0xFF0072CE),
    CHARGE_ZONE("ChargeZone", 0xFF00B0FF),
    STATIQ("Statiq", 0xFFFF6D00),
    JIO_BP("Jio-bp", 0xFF00897B),
    ZEON("Zeon Charging", 0xFF7C4DFF),
    BPCL("BPCL eDrive", 0xFFFFD600),
    KAZAM("Kazam", 0xFFE040FB),
    ATHER_GRID("Ather Grid", 0xFF00E676)
}

enum class ConnectorType(val displayName: String, val isFast: Boolean) {
    CCS_2("CCS 2", true),
    TYPE_2("Type 2", false),
    CHADEMO("CHAdeMO", true),
    AC_TYPE_1("AC 7.4kW", false),
    BHARAT_DC_001("Bharat DC", true)
}

enum class StationStatus(val label: String) {
    AVAILABLE("Available"),
    BUSY("Busy"),
    OFFLINE("Offline")
}

data class ConnectorInfo(
    val id: String,
    val type: ConnectorType,
    val powerKw: Int,
    val status: StationStatus,
    val pricePerKwh: Double,
    val availableCount: Int = 1,
    val totalCount: Int = 2
)

data class ChargingStation(
    val id: String,
    val name: String,
    val network: ChargingNetwork,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String = "Indore",
    val distanceKm: Double,
    val etaMinutes: Int,
    val rating: Double,
    val reviewsCount: Int,
    val pricePerKwh: Double,
    val status: StationStatus,
    val totalAvailable: Int,
    val totalPorts: Int,
    val maxPowerKw: Int,
    val connectors: List<ConnectorInfo>,
    val amenities: List<String> = listOf("Parking", "Cafeteria", "Restroom", "Wi-Fi", "24/7 Support"),
    val openHours: String = "24/7 Open",
    val isBookmarked: Boolean = false,
    val operatorVerified: Boolean = true
)

data class EvVehicle(
    val id: String,
    val make: String,
    val model: String,
    val batteryCapacityKwh: Double,
    val realWorldRangeKm: Int,
    val connectorType: ConnectorType,
    val currentBatteryPercent: Int = 68,
    val isDefault: Boolean = true
)

data class UserProfile(
    val id: String = "user_101",
    val name: String = "Binayak Tiwari",
    val email: String = "binayaktiwari77@gmail.com",
    val phone: String = "+91 98765 43210",
    val city: String = "Indore, Madhya Pradesh",
    val isPhoneVerified: Boolean = true,
    val isGoogleLinked: Boolean = true
)

data class ChargingSession(
    val id: String,
    val stationId: String,
    val stationName: String,
    val network: ChargingNetwork,
    val connectorType: ConnectorType,
    val powerKw: Int,
    val currentPercent: Float,
    val targetPercent: Float = 85f,
    val energyAddedKwh: Double,
    val totalCostRupees: Double,
    val estMinutesRemaining: Int,
    val isLive: Boolean = true,
    val startTimeFormatted: String = "Today, 10:15 AM",
    val evseId: String = "EVSE-08",
    val txnId: String = "TXN-839201",
    val paymentMethodName: String = "UPI (Google Pay)"
)

data class TripRecord(
    val id: String,
    val origin: String,
    val destination: String,
    val distanceKm: Int,
    val durationFormatted: String,
    val costRupees: Int,
    val dateFormatted: String,
    val status: String = "Completed",
    val stopsCount: Int = 1
)

data class SavedLocation(
    val id: String,
    val label: String,
    val address: String,
    val iconName: String = "home"
)

data class RecommendedChargingStop(
    val station: ChargingStation,
    val distanceFromStartKm: Int,
    val drivingTimeMin: Int,
    val suggestedChargeMin: Int,
    val arrivalBatteryPercent: Int,
    val departureBatteryPercent: Int,
    val estimatedCostRupees: Int
)

data class RoutePlan(
    val origin: String,
    val destination: String,
    val distanceKm: Int,
    val durationFormatted: String,
    val stops: List<RecommendedChargingStop>,
    val startBatteryPercent: Int,
    val estimatedArrivalBatteryPercent: Int,
    val totalEstimatedCost: Int
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val type: String = "charging"
)

// --- QR-Based Charging Models ---

enum class PaymentMethod(val title: String, val subtitle: String, val iconType: String) {
    UPI("UPI Transfer", "Google Pay, PhonePe, Paytm, BHIM", "upi"),
    WALLET("VoltElite Wallet", "Available Balance: ₹1,500.00", "wallet"),
    CARD("Credit / Debit Card", "Visa ending in 4821", "card"),
    PRE_AUTH("Pre-Authorization Hold", "₹500 temporary hold released on completion", "shield")
}

data class ParsedQrCharger(
    val provider: ChargingNetwork,
    val stationId: String,
    val stationName: String,
    val evseId: String,
    val connectorId: String,
    val connectorType: ConnectorType,
    val powerKw: Int,
    val tariffPerKwh: Double,
    val location: String,
    val status: StationStatus,
    val supportsRemoteStart: Boolean,
    val providerDeepLink: String? = null,
    val rawPayload: String
)

data class VehicleCompatibility(
    val isCompatible: Boolean,
    val userVehicleMakeModel: String,
    val userVehicleConnector: ConnectorType,
    val chargerConnector: ConnectorType,
    val message: String
)

data class ChargerVerificationResult(
    val charger: ParsedQrCharger,
    val compatibility: VehicleCompatibility,
    val estimatedFullCost: Int,
    val estimatedDurationMin: Int
)

data class ChargingReceipt(
    val sessionId: String,
    val txnId: String,
    val stationName: String,
    val evseId: String,
    val network: ChargingNetwork,
    val connectorType: ConnectorType,
    val powerKw: Int,
    val energyDeliveredKwh: Double,
    val chargingTimeMinutes: Int,
    val totalAmountRupees: Double,
    val tariffPerKwh: Double,
    val paymentMethod: String,
    val timestampFormatted: String,
    val invoiceNumber: String
)
