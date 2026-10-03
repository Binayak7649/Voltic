package com.example.data.provider

import com.example.model.*

/**
 * ChargingProvider Adapter Architecture
 * Each EV charging network integration communicates through this standard protocol interface.
 * Implements real network metadata, roaming protocol identifiers, and fallback demo telemetry.
 */
interface ChargingProvider {
    val network: ChargingNetwork
    val roamingProtocolVersion: String
    val isPartnerApiAvailable: Boolean

    suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation>
    suspend fun getStationDetails(stationId: String): ChargingStation?
    suspend fun checkAvailability(stationId: String): StationStatus
    suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String>
    suspend fun stopRemoteSession(sessionId: String): Result<Boolean>
}

// 1. Tata Power Provider Adapter (EZ Charge network)
class TataPowerProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.TATA_POWER
    override val roamingProtocolVersion: String = "OCPI 2.2.1 / Tata EZ-API v3"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "tp_indore_01",
                name = "Tata Power Charging Station",
                network = ChargingNetwork.TATA_POWER,
                latitude = 22.7533,
                longitude = 75.8937,
                address = "Near Vijay Nagar Square, AB Road",
                city = "Indore",
                distanceKm = 0.8,
                etaMinutes = 5,
                rating = 4.6,
                reviewsCount = 128,
                pricePerKwh = 18.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 6,
                totalPorts = 8,
                maxPowerKw = 60,
                connectors = listOf(
                    ConnectorInfo("c1", ConnectorType.CCS_2, 60, StationStatus.AVAILABLE, 18.0, 3, 4),
                    ConnectorInfo("c2", ConnectorType.TYPE_2, 22, StationStatus.AVAILABLE, 15.0, 2, 2),
                    ConnectorInfo("c3", ConnectorType.AC_TYPE_1, 7, StationStatus.AVAILABLE, 12.0, 1, 2)
                ),
                amenities = listOf("Parking", "Cafeteria", "Restroom", "Wi-Fi", "24/7 Security")
            ),
            ChargingStation(
                id = "tp_dewas_02",
                name = "Tata Power — Dewas Bypass Hub",
                network = ChargingNetwork.TATA_POWER,
                latitude = 22.9676,
                longitude = 76.0534,
                address = "NH 52, Dewas Bypass Highway Plaza",
                city = "Dewas",
                distanceKm = 52.0,
                etaMinutes = 45,
                rating = 4.7,
                reviewsCount = 94,
                pricePerKwh = 19.5,
                status = StationStatus.AVAILABLE,
                totalAvailable = 4,
                totalPorts = 6,
                maxPowerKw = 120,
                connectors = listOf(
                    ConnectorInfo("c4", ConnectorType.CCS_2, 120, StationStatus.AVAILABLE, 19.5, 2, 2),
                    ConnectorInfo("c5", ConnectorType.CCS_2, 60, StationStatus.AVAILABLE, 18.0, 2, 4)
                ),
                amenities = listOf("Highway Food Court", "Parking", "Restroom", "24/7")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.7, 75.8, 100.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("tp_sess_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 2. ChargeZone Provider Adapter
class ChargeZoneProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.CHARGE_ZONE
    override val roamingProtocolVersion: String = "OCPI 2.2 / ChargeZone Direct"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "cz_indore_01",
                name = "ChargeZone — Phoenix Citadel Mall",
                network = ChargingNetwork.CHARGE_ZONE,
                latitude = 22.7244,
                longitude = 75.8839,
                address = "Basement B2, Phoenix Citadel Mall, Bypass Rd",
                city = "Indore",
                distanceKm = 2.4,
                etaMinutes = 10,
                rating = 4.8,
                reviewsCount = 210,
                pricePerKwh = 21.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 8,
                totalPorts = 10,
                maxPowerKw = 120,
                connectors = listOf(
                    ConnectorInfo("cz1", ConnectorType.CCS_2, 120, StationStatus.AVAILABLE, 21.0, 4, 4),
                    ConnectorInfo("cz2", ConnectorType.TYPE_2, 22, StationStatus.AVAILABLE, 16.0, 4, 6)
                ),
                amenities = listOf("Shopping Mall", "Valet Parking", "Food Court", "Cinema", "Wi-Fi")
            ),
            ChargingStation(
                id = "cz_ujjain_02",
                name = "ChargeZone — Ujjain Highway Express",
                network = ChargingNetwork.CHARGE_ZONE,
                latitude = 23.1765,
                longitude = 75.7885,
                address = "Sanwer-Ujjain Expressway, Toll Junction",
                city = "Ujjain",
                distanceKm = 98.0,
                etaMinutes = 95,
                rating = 4.5,
                reviewsCount = 76,
                pricePerKwh = 20.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 3,
                totalPorts = 4,
                maxPowerKw = 60,
                connectors = listOf(
                    ConnectorInfo("cz3", ConnectorType.CCS_2, 60, StationStatus.AVAILABLE, 20.0, 3, 4)
                ),
                amenities = listOf("Restaurant", "Restroom", "EV Lounge", "24/7")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.7, 75.8, 100.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("cz_sess_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 3. Statiq Provider Adapter
class StatiqProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.STATIQ
    override val roamingProtocolVersion: String = "OCPI 2.2"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "statiq_indore_01",
                name = "Statiq EV Station — Palasia",
                network = ChargingNetwork.STATIQ,
                latitude = 22.7240,
                longitude = 75.8820,
                address = "Near Industry House, Old Palasia",
                city = "Indore",
                distanceKm = 1.9,
                etaMinutes = 7,
                rating = 4.4,
                reviewsCount = 89,
                pricePerKwh = 17.5,
                status = StationStatus.AVAILABLE,
                totalAvailable = 3,
                totalPorts = 4,
                maxPowerKw = 50,
                connectors = listOf(
                    ConnectorInfo("sq1", ConnectorType.CCS_2, 50, StationStatus.AVAILABLE, 17.5, 2, 2),
                    ConnectorInfo("sq2", ConnectorType.TYPE_2, 11, StationStatus.AVAILABLE, 14.0, 1, 2)
                ),
                amenities = listOf("Coffee Shop", "Parking", "Wi-Fi")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.7, 75.8, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("statiq_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 4. Jio-bp Pulse Provider Adapter
class JioBPProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.JIO_BP
    override val roamingProtocolVersion: String = "Jio-bp Pulse Roaming v2.1"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "jiobp_indore_01",
                name = "Jio-bp pulse Station — Super Corridor",
                network = ChargingNetwork.JIO_BP,
                latitude = 22.7667,
                longitude = 75.8234,
                address = "Super Corridor Airport Rd, TCS Square",
                city = "Indore",
                distanceKm = 4.2,
                etaMinutes = 14,
                rating = 4.7,
                reviewsCount = 145,
                pricePerKwh = 19.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 5,
                totalPorts = 6,
                maxPowerKw = 60,
                connectors = listOf(
                    ConnectorInfo("jbp1", ConnectorType.CCS_2, 60, StationStatus.AVAILABLE, 19.0, 4, 4),
                    ConnectorInfo("jbp2", ConnectorType.TYPE_2, 22, StationStatus.AVAILABLE, 15.0, 1, 2)
                ),
                amenities = listOf("Wild Bean Cafe", "Restroom", "24/7", "Air & Water", "Convenience Store")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.7, 75.8, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("jbp_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 5. Zeon Charging Provider Adapter
class ZeonProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.ZEON
    override val roamingProtocolVersion: String = "Zeon Rapid OCPI 2.2"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "zeon_bhopal_01",
                name = "Zeon HyperCharge — Bhopal Highway",
                network = ChargingNetwork.ZEON,
                latitude = 23.2330,
                longitude = 77.4343,
                address = "Hoshangabad Rd, Near Capital Mall",
                city = "Bhopal",
                distanceKm = 190.0,
                etaMinutes = 180,
                rating = 4.9,
                reviewsCount = 312,
                pricePerKwh = 22.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 4,
                totalPorts = 4,
                maxPowerKw = 150,
                connectors = listOf(
                    ConnectorInfo("zn1", ConnectorType.CCS_2, 150, StationStatus.AVAILABLE, 22.0, 4, 4)
                ),
                amenities = listOf("EV Lounge", "Fast Food", "Restroom", "24/7 Security")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(23.2, 77.4, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("zeon_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 6. BPCL eDrive Provider Adapter
class BPCLProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.BPCL
    override val roamingProtocolVersion: String = "BPCL eDrive API v1.9"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "bpcl_bhopal_02",
                name = "BPCL eDrive — Bhopal Bypass",
                network = ChargingNetwork.BPCL,
                latitude = 23.2599,
                longitude = 77.4126,
                address = "BPCL Highway Fuel Point, Bhopal Bypass NH 46",
                city = "Bhopal",
                distanceKm = 155.0,
                etaMinutes = 155,
                rating = 4.3,
                reviewsCount = 64,
                pricePerKwh = 16.5,
                status = StationStatus.AVAILABLE,
                totalAvailable = 2,
                totalPorts = 4,
                maxPowerKw = 30,
                connectors = listOf(
                    ConnectorInfo("bp1", ConnectorType.CCS_2, 30, StationStatus.AVAILABLE, 16.5, 2, 2),
                    ConnectorInfo("bp2", ConnectorType.BHARAT_DC_001, 15, StationStatus.AVAILABLE, 14.0, 0, 2)
                ),
                amenities = listOf("Fuel Station", "Restroom", "24/7 Tyre Care")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(23.2, 77.4, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("bpcl_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 7. Kazam Provider Adapter
class KazamProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.KAZAM
    override val roamingProtocolVersion: String = "Kazam IoT Cloud"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "kazam_indore_01",
                name = "Kazam EV Hub — Bhawarkuan",
                network = ChargingNetwork.KAZAM,
                latitude = 22.6926,
                longitude = 75.8676,
                address = "Bhawarkuan Main Square, University Road",
                city = "Indore",
                distanceKm = 3.6,
                etaMinutes = 12,
                rating = 4.2,
                reviewsCount = 42,
                pricePerKwh = 16.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 4,
                totalPorts = 4,
                maxPowerKw = 22,
                connectors = listOf(
                    ConnectorInfo("kz1", ConnectorType.TYPE_2, 22, StationStatus.AVAILABLE, 16.0, 2, 2),
                    ConnectorInfo("kz2", ConnectorType.AC_TYPE_1, 7, StationStatus.AVAILABLE, 13.0, 2, 2)
                ),
                amenities = listOf("Student Hub", "Parking", "Cafeteria")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.6, 75.8, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.AVAILABLE
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("kazam_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}

// 8. Ather Grid Provider Adapter
class AtherGridProvider : ChargingProvider {
    override val network: ChargingNetwork = ChargingNetwork.ATHER_GRID
    override val roamingProtocolVersion: String = "Ather Grid Open Network v1"
    override val isPartnerApiAvailable: Boolean = true

    override suspend fun getStations(lat: Double, lng: Double, radiusKm: Double): List<ChargingStation> {
        return listOf(
            ChargingStation(
                id = "ather_indore_01",
                name = "Ather Grid — Treasure Island Mall",
                network = ChargingNetwork.ATHER_GRID,
                latitude = 22.7214,
                longitude = 75.8790,
                address = "MG Road, Treasure Island Mall Parking",
                city = "Indore",
                distanceKm = 1.4,
                etaMinutes = 6,
                rating = 4.9,
                reviewsCount = 380,
                pricePerKwh = 15.0,
                status = StationStatus.BUSY,
                totalAvailable = 1,
                totalPorts = 4,
                maxPowerKw = 22,
                connectors = listOf(
                    ConnectorInfo("ath1", ConnectorType.TYPE_2, 22, StationStatus.BUSY, 15.0, 1, 4)
                ),
                amenities = listOf("Shopping", "Food Court", "Restroom")
            )
        )
    }

    override suspend fun getStationDetails(stationId: String): ChargingStation? =
        getStations(22.7, 75.8, 50.0).find { it.id == stationId }

    override suspend fun checkAvailability(stationId: String): StationStatus = StationStatus.BUSY
    override suspend fun startRemoteSession(stationId: String, connectorId: String): Result<String> =
        Result.success("ather_${System.currentTimeMillis()}")
    override suspend fun stopRemoteSession(sessionId: String): Result<Boolean> = Result.success(true)
}
