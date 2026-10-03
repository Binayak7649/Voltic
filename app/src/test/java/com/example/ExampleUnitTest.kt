package com.example

import com.example.data.provider.*
import com.example.data.service.QRChargingService
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testProviderAdaptersIntegrity() {
        val providers: List<ChargingProvider> = listOf(
            TataPowerProvider(),
            ChargeZoneProvider(),
            StatiqProvider(),
            JioBPProvider(),
            ZeonProvider(),
            BPCLProvider(),
            KazamProvider(),
            AtherGridProvider()
        )
        assertEquals(8, providers.size)
        assertTrue(providers.all { it.isPartnerApiAvailable })
    }

    @Test
    fun testTripEstimationAlgorithm() {
        val distanceKm = 250
        val rangeKm = 180
        val currentPercent = 68

        val usableInitialRange = (rangeKm * (currentPercent / 100.0)).toInt()
        val remaining = distanceKm - usableInitialRange
        val stops = ((remaining / (rangeKm * 0.7)).toInt() + 1).coerceAtMost(5)

        assertEquals(2, stops)
    }

    @Test
    fun testQrParserVoltEliteDeepLink() {
        val qrService = QRChargingService()
        val payload = "VOLT-DEMO-STN001-EVSE08-CCS2"
        val result = qrService.parseQr(payload)

        assertTrue(result.isSuccess)
        val parsed = result.getOrNull()
        assertNotNull(parsed)
        assertEquals(ChargingNetwork.TATA_POWER, parsed?.provider)
        assertEquals("EVSE-08", parsed?.evseId)
        assertEquals(ConnectorType.CCS_2, parsed?.connectorType)
        assertEquals(60, parsed?.powerKw)
        assertTrue(parsed?.supportsRemoteStart == true)
    }

    @Test
    fun testQrParserChargeZonePreset() {
        val qrService = QRChargingService()
        val payload = "CZ-PHX-120KW-CCS2"
        val result = qrService.parseQr(payload)

        assertTrue(result.isSuccess)
        val parsed = result.getOrNull()
        assertNotNull(parsed)
        assertEquals(ChargingNetwork.CHARGE_ZONE, parsed?.provider)
        assertEquals(120, parsed?.powerKw)
        assertEquals(21.0, parsed?.tariffPerKwh ?: 0.0, 0.01)
    }

    @Test
    fun testQrParserInvalidPayload() {
        val qrService = QRChargingService()
        val result = qrService.parseQr("")
        assertTrue(result.isFailure)
    }

    @Test
    fun testVehicleCompatibility() {
        val qrService = QRChargingService()
        val ccsVehicle = EvVehicle(
            id = "v1",
            make = "Tata",
            model = "Nexon EV",
            batteryCapacityKwh = 40.5,
            realWorldRangeKm = 312,
            connectorType = ConnectorType.CCS_2
        )
        val type2Vehicle = EvVehicle(
            id = "v2",
            make = "Mahindra",
            model = "eVerito",
            batteryCapacityKwh = 21.2,
            realWorldRangeKm = 140,
            connectorType = ConnectorType.TYPE_2
        )

        val ccsCharger = qrService.parseQr("VOLT-DEMO-STN001-EVSE08-CCS2").getOrThrow()

        val verifiedCcs = qrService.verifyCharger(ccsCharger, ccsVehicle)
        assertTrue(verifiedCcs.compatibility.isCompatible)

        val verifiedMismatch = qrService.verifyCharger(ccsCharger, type2Vehicle)
        assertFalse(verifiedMismatch.compatibility.isCompatible)
    }
}
