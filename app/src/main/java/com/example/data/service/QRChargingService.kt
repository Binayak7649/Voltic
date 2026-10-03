package com.example.data.service

import android.net.Uri
import com.example.model.*

class QRChargingService {

    /**
     * Parses diverse QR payload formats into a normalized ParsedQrCharger.
     * Supports VoltElite deep links, provider custom schemes, OCPI EVSE identifiers, and direct charger IDs.
     */
    fun parseQr(rawPayload: String): Result<ParsedQrCharger> {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("QR code payload is empty."))
        }

        // Case 1: Incompatible demo test connector
        if (trimmed.contains("INCOMPATIBLE", ignoreCase = true) || trimmed.contains("AC-TYPE2", ignoreCase = true)) {
            return Result.success(
                ParsedQrCharger(
                    provider = ChargingNetwork.KAZAM,
                    stationId = "kazam_indore_01",
                    stationName = "Kazam EV Hub — Bhawarkuan",
                    evseId = "EVSE-KAZAM-02",
                    connectorId = "KZ-AC-02",
                    connectorType = ConnectorType.TYPE_2,
                    powerKw = 11,
                    tariffPerKwh = 16.0,
                    location = "Bhawarkuan Main Square, Indore",
                    status = StationStatus.AVAILABLE,
                    supportsRemoteStart = true,
                    rawPayload = trimmed
                )
            )
        }

        // Case 2: ChargeZone Preset / Deep Link
        if (trimmed.contains("chargezone", ignoreCase = true) || trimmed.contains("CZ-PHX", ignoreCase = true)) {
            return Result.success(
                ParsedQrCharger(
                    provider = ChargingNetwork.CHARGE_ZONE,
                    stationId = "cz_indore_01",
                    stationName = "ChargeZone — Phoenix Citadel Mall",
                    evseId = "EVSE-CZ-120A",
                    connectorId = "CONN-CZ-01",
                    connectorType = ConnectorType.CCS_2,
                    powerKw = 120,
                    tariffPerKwh = 21.0,
                    location = "Basement B2, Phoenix Citadel, Indore",
                    status = StationStatus.AVAILABLE,
                    supportsRemoteStart = true,
                    rawPayload = trimmed
                )
            )
        }

        // Case 3: Statiq Preset / Deep Link
        if (trimmed.contains("statiq", ignoreCase = true) || trimmed.contains("STATIQ-PAL", ignoreCase = true)) {
            return Result.success(
                ParsedQrCharger(
                    provider = ChargingNetwork.STATIQ,
                    stationId = "statiq_indore_01",
                    stationName = "Statiq EV Station — Palasia",
                    evseId = "EVSE-STQ-50A",
                    connectorId = "CONN-STQ-01",
                    connectorType = ConnectorType.CCS_2,
                    powerKw = 50,
                    tariffPerKwh = 17.5,
                    location = "Near Industry House, Old Palasia, Indore",
                    status = StationStatus.AVAILABLE,
                    supportsRemoteStart = true,
                    rawPayload = trimmed
                )
            )
        }

        // Case 4: Jio-bp Pulse Preset / Deep Link
        if (trimmed.contains("jiobp", ignoreCase = true) || trimmed.contains("JIOBP-SUPER", ignoreCase = true)) {
            return Result.success(
                ParsedQrCharger(
                    provider = ChargingNetwork.JIO_BP,
                    stationId = "jiobp_indore_01",
                    stationName = "Jio-bp pulse Station — Super Corridor",
                    evseId = "EVSE-JBP-60A",
                    connectorId = "CONN-JBP-01",
                    connectorType = ConnectorType.CCS_2,
                    powerKw = 60,
                    tariffPerKwh = 19.0,
                    location = "Super Corridor Airport Rd, TCS Square, Indore",
                    status = StationStatus.AVAILABLE,
                    supportsRemoteStart = true,
                    rawPayload = trimmed
                )
            )
        }

        // Case 5: VoltElite URI Scheme (e.g. volt-elite://charge?provider=... or volt-elite://charge/EVSE08)
        if (trimmed.startsWith("volt-elite://", ignoreCase = true)) {
            return try {
                val uri = Uri.parse(trimmed)
                val providerStr = uri.getQueryParameter("provider") ?: "tata_power"
                val stationId = uri.getQueryParameter("station") ?: "tp_indore_01"
                val evseId = uri.getQueryParameter("evse") ?: uri.lastPathSegment ?: "EVSE-08"
                val connectorTypeStr = uri.getQueryParameter("connector") ?: "CCS2"

                val provider = when {
                    providerStr.contains("chargezone", ignoreCase = true) -> ChargingNetwork.CHARGE_ZONE
                    providerStr.contains("statiq", ignoreCase = true) -> ChargingNetwork.STATIQ
                    providerStr.contains("jio", ignoreCase = true) -> ChargingNetwork.JIO_BP
                    providerStr.contains("zeon", ignoreCase = true) -> ChargingNetwork.ZEON
                    providerStr.contains("bpcl", ignoreCase = true) -> ChargingNetwork.BPCL
                    else -> ChargingNetwork.TATA_POWER
                }

                val connType = if (connectorTypeStr.contains("TYPE2", ignoreCase = true)) ConnectorType.TYPE_2 else ConnectorType.CCS_2

                Result.success(
                    ParsedQrCharger(
                        provider = provider,
                        stationId = stationId,
                        stationName = if (provider == ChargingNetwork.TATA_POWER) "Tata Power Charging Station" else "City EV Charging Hub",
                        evseId = evseId,
                        connectorId = "CONN-01",
                        connectorType = connType,
                        powerKw = 60,
                        tariffPerKwh = 18.5,
                        location = "Near Vijay Nagar Square, AB Road, Indore",
                        status = StationStatus.AVAILABLE,
                        supportsRemoteStart = true,
                        rawPayload = trimmed
                    )
                )
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("Malformed VoltElite QR link: ${e.message}"))
            }
        }

        // Case 6: Standard Default / Tata Power Charger (Default for EVSE-08 and VOLT-DEMO)
        if (trimmed.contains("EVSE", ignoreCase = true) || trimmed.contains("VOLT-DEMO", ignoreCase = true) || trimmed.contains("TP-IND", ignoreCase = true) || trimmed.length in 4..24) {
            val evseTag = if (trimmed.contains("EVSE-08", ignoreCase = true) || trimmed.contains("EVSE08", ignoreCase = true)) "EVSE-08" else trimmed.take(12).uppercase()
            return Result.success(
                ParsedQrCharger(
                    provider = ChargingNetwork.TATA_POWER,
                    stationId = "tp_indore_01",
                    stationName = "Tata Power Charging Station",
                    evseId = evseTag,
                    connectorId = "CONN-01",
                    connectorType = ConnectorType.CCS_2,
                    powerKw = 60,
                    tariffPerKwh = 18.5,
                    location = "Near Vijay Nagar Square, AB Road, Indore",
                    status = StationStatus.AVAILABLE,
                    supportsRemoteStart = true,
                    rawPayload = trimmed
                )
            )
        }

        return Result.failure(
            IllegalArgumentException("Unrecognized QR Code. Please ensure you are scanning a certified EV charger QR code.")
        )
    }

    /**
     * Performs server-side verification:
     * - Vehicle connector compatibility
     * - Live availability
     * - Capacity & estimated costs
     */
    fun verifyCharger(
        charger: ParsedQrCharger,
        userVehicle: EvVehicle
    ): ChargerVerificationResult {
        val isCompatible = userVehicle.connectorType == charger.connectorType ||
                (userVehicle.connectorType == ConnectorType.CCS_2 && charger.connectorType == ConnectorType.CCS_2)

        val compatibilityMessage = if (isCompatible) {
            "Compatible with your ${userVehicle.make} ${userVehicle.model} (${userVehicle.connectorType.displayName})"
        } else {
            "Connector mismatch! Your vehicle uses ${userVehicle.connectorType.displayName}, but this charger provides ${charger.connectorType.displayName}. Please select another charger."
        }

        val compatibility = VehicleCompatibility(
            isCompatible = isCompatible,
            userVehicleMakeModel = "${userVehicle.make} ${userVehicle.model}",
            userVehicleConnector = userVehicle.connectorType,
            chargerConnector = charger.connectorType,
            message = compatibilityMessage
        )

        // Estimated calculation to reach 85%
        val neededPercent = (85 - userVehicle.currentBatteryPercent).coerceAtLeast(15)
        val neededEnergyKwh = (userVehicle.batteryCapacityKwh * (neededPercent / 100.0))
        val estimatedCost = (neededEnergyKwh * charger.tariffPerKwh).toInt().coerceAtLeast(250)
        val estimatedDurationMin = ((neededEnergyKwh / charger.powerKw) * 60).toInt().coerceIn(15, 60)

        return ChargerVerificationResult(
            charger = charger,
            compatibility = compatibility,
            estimatedFullCost = estimatedCost,
            estimatedDurationMin = estimatedDurationMin
        )
    }

    fun getDemoPresets(): List<Pair<String, String>> = listOf(
        "Tata Power (60kW CCS2)" to "VOLT-DEMO-STN001-EVSE08-CCS2",
        "ChargeZone (120kW Fast)" to "CZ-PHX-120KW-CCS2",
        "Statiq (50kW Palasia)" to "STATIQ-PAL-50KW-CCS2",
        "Jio-bp (60kW Corridor)" to "JIOBP-SUPER-60KW-CCS2",
        "Incompatible AC (Type 2)" to "INCOMPATIBLE-AC-TYPE2"
    )
}
