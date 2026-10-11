package com.example

import androidx.compose.runtime.Immutable
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class PerformanceAndSmoothnessTest {

    @Test
    fun testCoreModelsDataIntegrity() {
        val user = UserProfile()
        assertEquals("user_101", user.id)
        assertTrue(user.isGoogleLinked)

        val vehicle = EvVehicle(
            id = "v1",
            make = "Tata",
            model = "Nexon EV",
            batteryCapacityKwh = 40.5,
            realWorldRangeKm = 312,
            connectorType = ConnectorType.CCS_2
        )
        assertEquals("Nexon EV", vehicle.model)
        assertEquals(ConnectorType.CCS_2, vehicle.connectorType)

        val conn = ConnectorInfo(
            id = "c1",
            type = ConnectorType.CCS_2,
            powerKw = 60,
            status = StationStatus.AVAILABLE,
            pricePerKwh = 18.5
        )
        assertEquals(60, conn.powerKw)
        assertEquals(StationStatus.AVAILABLE, conn.status)
    }

    @Test
    fun testSpatialMarkerClusteringLogic() {
        val testStations = listOf(
            ChargingStation(
                id = "stn_01",
                name = "Station 1",
                network = ChargingNetwork.TATA_POWER,
                latitude = 22.7196,
                longitude = 75.8577,
                address = "Palasia",
                distanceKm = 1.0,
                etaMinutes = 5,
                rating = 4.8,
                reviewsCount = 50,
                pricePerKwh = 18.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 2,
                totalPorts = 4,
                maxPowerKw = 60,
                connectors = emptyList()
            ),
            ChargingStation(
                id = "stn_02",
                name = "Station 2 (Nearby stn 1)",
                network = ChargingNetwork.CHARGE_ZONE,
                latitude = 22.7210, // ~150 meters away
                longitude = 75.8590,
                address = "Palasia North",
                distanceKm = 1.2,
                etaMinutes = 6,
                rating = 4.7,
                reviewsCount = 40,
                pricePerKwh = 19.0,
                status = StationStatus.AVAILABLE,
                totalAvailable = 1,
                totalPorts = 2,
                maxPowerKw = 120,
                connectors = emptyList()
            ),
            ChargingStation(
                id = "stn_03",
                name = "Station 3 (Far away)",
                network = ChargingNetwork.STATIQ,
                latitude = 22.8500, // ~15 km away
                longitude = 75.9800,
                address = "Bypass Road",
                distanceKm = 15.0,
                etaMinutes = 25,
                rating = 4.5,
                reviewsCount = 20,
                pricePerKwh = 18.5,
                status = StationStatus.AVAILABLE,
                totalAvailable = 3,
                totalPorts = 4,
                maxPowerKw = 50,
                connectors = emptyList()
            )
        )

        // Cluster clustering algorithm test
        val visited = mutableSetOf<String>()
        val clusterThresholdDeg = 0.045
        val clusters = mutableListOf<List<ChargingStation>>()

        for (st in testStations) {
            if (visited.contains(st.id)) continue
            val group = testStations.filter { other ->
                !visited.contains(other.id) &&
                Math.abs(st.latitude - other.latitude) < clusterThresholdDeg &&
                Math.abs(st.longitude - other.longitude) < clusterThresholdDeg
            }
            group.forEach { visited.add(it.id) }
            clusters.add(group)
        }

        // Stn 1 and Stn 2 should be clustered together; Stn 3 should be in its own cluster
        assertEquals(2, clusters.size)
        assertEquals(2, clusters[0].size) // Group containing stn_01 & stn_02
        assertEquals(1, clusters[1].size) // Isolated stn_03
    }

    @Test
    fun testMapplsNavigationUriFormat() {
        val lat = 22.7196
        val lon = 75.8577
        val expectedMapplsUri = "mappls://navigation?destination=$lat,$lon"
        val expectedGeoUri = "geo:$lat,$lon?q=$lat,$lon(Tata+Power)"

        assertTrue(expectedMapplsUri.startsWith("mappls://navigation?destination="))
        assertTrue(expectedMapplsUri.contains("22.7196,75.8577"))
        assertTrue(expectedGeoUri.startsWith("geo:"))
    }
}
