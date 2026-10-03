package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.provider.*
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UnifiedChargingRepository(
    private val context: Context,
    private val database: VoltEliteDatabase
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Provider Adapters for all 8 major EV networks
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

    private val _allStations = MutableStateFlow<List<ChargingStation>>(emptyList())
    val allStations: StateFlow<List<ChargingStation>> = _allStations.asStateFlow()

    // Active Charging Session Simulation
    private val _activeSession = MutableStateFlow<ChargingSession?>(null)
    val activeSession: StateFlow<ChargingSession?> = _activeSession.asStateFlow()

    // Room Database Flows
    val vehicles: Flow<List<VehicleEntity>> = database.vehicleDao().getAllVehicles()
    val trips: Flow<List<TripEntity>> = database.tripDao().getAllTrips()
    val savedLocations: Flow<List<SavedLocationEntity>> = database.savedLocationDao().getAllSavedLocations()
    val pastSessions: Flow<List<ChargingSessionEntity>> = database.chargingSessionDao().getAllSessions()
    val notifications: Flow<List<NotificationEntity>> = database.notificationDao().getAllNotifications()
    val bookmarkedIds: Flow<List<String>> = database.stationBookmarkDao().getBookmarkedStationIds()

    init {
        coroutineScope.launch {
            loadAllStations()
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun loadAllStations() {
        val aggregated = mutableListOf<ChargingStation>()
        providers.forEach { provider ->
            try {
                val list = provider.getStations(22.7196, 75.8577, 100.0)
                aggregated.addAll(list)
            } catch (e: Exception) {
                // Provider fault tolerance
            }
        }
        _allStations.value = aggregated
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Seed default vehicles if empty
        val vehicleDao = database.vehicleDao()
        vehicleDao.insertVehicle(
            VehicleEntity(
                id = "veh_01",
                make = "Tata",
                model = "Nexon EV",
                batteryCapacityKwh = 40.5,
                realWorldRangeKm = 312,
                connectorType = ConnectorType.CCS_2.name,
                currentBatteryPercent = 68,
                isDefault = true
            )
        )
        vehicleDao.insertVehicle(
            VehicleEntity(
                id = "veh_02",
                make = "MG",
                model = "ZS EV",
                batteryCapacityKwh = 50.3,
                realWorldRangeKm = 380,
                connectorType = ConnectorType.CCS_2.name,
                currentBatteryPercent = 82,
                isDefault = false
            )
        )

        // Seed saved locations
        val locDao = database.savedLocationDao()
        locDao.insertLocation(SavedLocationEntity("loc_home", "Home", "Scheme 54, Vijay Nagar, Indore", "home"))
        locDao.insertLocation(SavedLocationEntity("loc_work", "Work (TCS / Infy Campus)", "Super Corridor, Indore", "work"))
        locDao.insertLocation(SavedLocationEntity("loc_fav", "Indore Tech Park", "Crystal IT Park, Ring Rd, Indore", "star"))

        // Seed trip history matching prompt
        val tripDao = database.tripDao()
        tripDao.insertTrip(TripEntity("tr_01", "Indore", "Bhopal", 196, "3 hr 12 min", 356, "02 Oct 2026", "Completed", 1))
        tripDao.insertTrip(TripEntity("tr_02", "Indore", "Ujjain", 56, "1 hr 05 min", 112, "30 Sep 2026", "Completed", 0))
        tripDao.insertTrip(TripEntity("tr_03", "Bhopal", "Indore", 196, "3 hr 08 min", 348, "28 Sep 2026", "Completed", 1))
        tripDao.insertTrip(TripEntity("tr_04", "Indore", "Pune", 710, "10 hr 45 min", 1248, "25 Sep 2026", "Completed", 3))

        // Seed notifications
        val notifDao = database.notificationDao()
        notifDao.insertNotification(NotificationEntity("n1", "Charging Complete! ⚡", "Tata Power station added 24.8 kWh. Your battery is at 85%.", "10m ago", false, "charging"))
        notifDao.insertNotification(NotificationEntity("n2", "New Fast Charger Near You 📍", "ChargeZone opened 8 new 120kW stalls at Phoenix Citadel.", "2h ago", false, "station"))
        notifDao.insertNotification(NotificationEntity("n3", "Trip Route Ready 🚗", "Your planned route Indore → Bhopal is optimized with 1 stop.", "1d ago", true, "trip"))

        // Seed initial charging session history
        val sessionDao = database.chargingSessionDao()
        sessionDao.insertSession(
            ChargingSessionEntity(
                id = "sess_seed_01",
                stationId = "tp_indore_01",
                stationName = "Tata Power Charging Station",
                networkName = "Tata Power",
                connectorType = "CCS 2",
                powerKw = 60,
                energyAddedKwh = 31.6,
                totalCostRupees = 412.0,
                status = "Completed",
                evseId = "EVSE-08",
                txnId = "TXN-839201",
                paymentMethod = "UPI (Google Pay)",
                durationMin = 32,
                tariffPerKwh = 18.5,
                timestamp = System.currentTimeMillis() - 86400000L
            )
        )
        sessionDao.insertSession(
            ChargingSessionEntity(
                id = "sess_seed_02",
                stationId = "cz_indore_01",
                stationName = "ChargeZone — Phoenix Citadel Mall",
                networkName = "ChargeZone",
                connectorType = "CCS 2",
                powerKw = 120,
                energyAddedKwh = 42.0,
                totalCostRupees = 882.0,
                status = "Completed",
                evseId = "EVSE-CZ-01",
                txnId = "TXN-729104",
                paymentMethod = "VoltElite Wallet",
                durationMin = 24,
                tariffPerKwh = 21.0,
                timestamp = System.currentTimeMillis() - 172800000L
            )
        )
    }

    suspend fun toggleBookmark(stationId: String, currentBookmarked: Boolean) {
        val dao = database.stationBookmarkDao()
        if (currentBookmarked) {
            dao.removeBookmark(stationId)
        } else {
            dao.addBookmark(StationBookmarkEntity(stationId))
        }
    }

    suspend fun addVehicle(make: String, model: String, capacity: Double, range: Int, connector: ConnectorType) {
        val id = "veh_${System.currentTimeMillis()}"
        database.vehicleDao().insertVehicle(
            VehicleEntity(
                id = id,
                make = make,
                model = model,
                batteryCapacityKwh = capacity,
                realWorldRangeKm = range,
                connectorType = connector.name,
                currentBatteryPercent = 100,
                isDefault = false
            )
        )
    }

    suspend fun setDefaultVehicle(id: String) {
        database.vehicleDao().clearDefault()
        database.vehicleDao().setDefault(id)
    }

    // Smart Route Planning Algorithm
    fun calculateRoute(
        origin: String,
        destination: String,
        currentRangeKm: Int,
        evModel: String
    ): RoutePlan {
        val totalDistanceKm = when {
            origin.contains("Indore", ignoreCase = true) && destination.contains("Bhopal", ignoreCase = true) -> 196
            origin.contains("Indore", ignoreCase = true) && destination.contains("Ujjain", ignoreCase = true) -> 56
            origin.contains("Indore", ignoreCase = true) && destination.contains("Pune", ignoreCase = true) -> 710
            else -> 196
        }

        val travelHours = totalDistanceKm / 60
        val travelMin = (totalDistanceKm % 60) * 60 / 60
        val durationFormatted = "${travelHours} hr ${travelMin.coerceAtLeast(12)} min"

        val stops = mutableListOf<RecommendedChargingStop>()
        val stations = _allStations.value

        if (totalDistanceKm > currentRangeKm * 0.8) {
            // Recommendation 1: Dewas Bypass Hub (Tata Power)
            val stop1Station = stations.find { it.id == "tp_dewas_02" } ?: stations.first()
            stops.add(
                RecommendedChargingStop(
                    station = stop1Station,
                    distanceFromStartKm = 52,
                    drivingTimeMin = 45,
                    suggestedChargeMin = 20,
                    arrivalBatteryPercent = 42,
                    departureBatteryPercent = 85,
                    estimatedCostRupees = 240
                )
            )

            // Recommendation 2: Ujjain Highway Express (ChargeZone)
            val stop2Station = stations.find { it.id == "cz_ujjain_02" } ?: stations.getOrNull(1) ?: stop1Station
            stops.add(
                RecommendedChargingStop(
                    station = stop2Station,
                    distanceFromStartKm = 98,
                    drivingTimeMin = 100,
                    suggestedChargeMin = 25,
                    arrivalBatteryPercent = 38,
                    departureBatteryPercent = 80,
                    estimatedCostRupees = 280
                )
            )

            // Recommendation 3: Bhopal Bypass Point (BPCL)
            val stop3Station = stations.find { it.id == "bpcl_bhopal_02" } ?: stop2Station
            stops.add(
                RecommendedChargingStop(
                    station = stop3Station,
                    distanceFromStartKm = 155,
                    drivingTimeMin = 155,
                    suggestedChargeMin = 15,
                    arrivalBatteryPercent = 28,
                    departureBatteryPercent = 70,
                    estimatedCostRupees = 190
                )
            )
        }

        return RoutePlan(
            origin = origin,
            destination = destination,
            distanceKm = totalDistanceKm,
            durationFormatted = durationFormatted,
            stops = stops,
            startBatteryPercent = 68,
            estimatedArrivalBatteryPercent = 55,
            totalEstimatedCost = stops.sumOf { it.estimatedCostRupees }.coerceAtLeast(350)
        )
    }

    // Fast Trip Estimator Service
    fun estimateTrip(
        distanceKm: Int,
        batteryRangeKm: Int,
        currentBatteryPercent: Int
    ): TripEstimateResult {
        val usableInitialRange = (batteryRangeKm * (currentBatteryPercent / 100.0)).toInt()
        val neededStops = if (distanceKm <= usableInitialRange) {
            0
        } else {
            val remainingDistance = distanceKm - usableInitialRange
            ((remainingDistance / (batteryRangeKm * 0.7)).toInt() + 1).coerceAtMost(5)
        }

        val chargingTimeMin = neededStops * 35
        val minCost = neededStops * 290
        val maxCost = neededStops * 360

        val hours = chargingTimeMin / 60
        val mins = chargingTimeMin % 60
        val chargeTimeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        return TripEstimateResult(
            stopsCount = neededStops,
            chargingTimeFormatted = if (neededStops == 0) "0 min (Direct Trip)" else chargeTimeFormatted,
            costRangeFormatted = if (neededStops == 0) "₹0" else "₹$minCost – ₹$maxCost",
            totalTimeFormatted = "${(distanceKm / 55)}h ${(distanceKm % 55) + chargingTimeMin}m"
        )
    }

    // Start Real/Simulated Charging Session
    fun startChargingSession(station: ChargingStation, connector: ConnectorInfo) {
        val session = ChargingSession(
            id = "sess_${System.currentTimeMillis()}",
            stationId = station.id,
            stationName = station.name,
            network = station.network,
            connectorType = connector.type,
            powerKw = connector.powerKw,
            currentPercent = 68f,
            targetPercent = 85f,
            energyAddedKwh = 24.8,
            totalCostRupees = 446.0,
            estMinutesRemaining = 18,
            isLive = true,
            evseId = "EVSE-08",
            txnId = "TXN-${(100000..999999).random()}",
            paymentMethodName = "UPI (Google Pay)"
        )
        _activeSession.value = session
        startSimulationLoop(station.pricePerKwh)
    }

    fun startChargingSessionFromQr(charger: ParsedQrCharger, paymentMethod: PaymentMethod): ChargingSession {
        val session = ChargingSession(
            id = "sess_${System.currentTimeMillis()}",
            stationId = charger.stationId,
            stationName = charger.stationName,
            network = charger.provider,
            connectorType = charger.connectorType,
            powerKw = charger.powerKw,
            currentPercent = 68f,
            targetPercent = 85f,
            energyAddedKwh = 0.5,
            totalCostRupees = charger.tariffPerKwh * 0.5,
            estMinutesRemaining = 25,
            isLive = true,
            evseId = charger.evseId,
            txnId = "TXN-${(100000..999999).random()}",
            paymentMethodName = paymentMethod.title
        )
        _activeSession.value = session
        startSimulationLoop(charger.tariffPerKwh)
        return session
    }

    private fun startSimulationLoop(pricePerKwh: Double) {
        coroutineScope.launch {
            while (_activeSession.value?.isLive == true) {
                delay(2500)
                val current = _activeSession.value ?: break
                if (current.currentPercent >= current.targetPercent) {
                    stopChargingSession()
                    break
                }
                val newPercent = (current.currentPercent + 1f).coerceAtMost(100f)
                val newEnergy = current.energyAddedKwh + 0.35
                val newCost = newEnergy * pricePerKwh
                val newMinutes = (current.estMinutesRemaining - 1).coerceAtLeast(1)
                _activeSession.value = current.copy(
                    currentPercent = newPercent,
                    energyAddedKwh = (newEnergy * 10).toInt() / 10.0,
                    totalCostRupees = (newCost * 10).toInt() / 10.0,
                    estMinutesRemaining = newMinutes
                )
            }
        }
    }

    suspend fun stopChargingSession(): ChargingReceipt? {
        val current = _activeSession.value ?: return null
        _activeSession.value = current.copy(isLive = false)

        val durationMin = 32
        val tariffPerKwh = if (current.energyAddedKwh > 0) current.totalCostRupees / current.energyAddedKwh else 18.5

        val receipt = ChargingReceipt(
            sessionId = current.id,
            txnId = current.txnId,
            stationName = current.stationName,
            evseId = current.evseId,
            network = current.network,
            connectorType = current.connectorType,
            powerKw = current.powerKw,
            energyDeliveredKwh = current.energyAddedKwh,
            chargingTimeMinutes = durationMin,
            totalAmountRupees = current.totalCostRupees,
            tariffPerKwh = (tariffPerKwh * 10).toInt() / 10.0,
            paymentMethod = current.paymentMethodName,
            timestampFormatted = "03 Oct 2026, 06:15 PM",
            invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(8)}"
        )

        // Save session record to database
        database.chargingSessionDao().insertSession(
            ChargingSessionEntity(
                id = current.id,
                stationId = current.stationId,
                stationName = current.stationName,
                networkName = current.network.displayName,
                connectorType = current.connectorType.displayName,
                powerKw = current.powerKw,
                energyAddedKwh = current.energyAddedKwh,
                totalCostRupees = current.totalCostRupees,
                status = "Completed",
                evseId = current.evseId,
                txnId = current.txnId,
                paymentMethod = current.paymentMethodName,
                durationMin = durationMin,
                tariffPerKwh = receipt.tariffPerKwh,
                timestamp = System.currentTimeMillis()
            )
        )

        return receipt
    }
}

data class TripEstimateResult(
    val stopsCount: Int,
    val chargingTimeFormatted: String,
    val costRangeFormatted: String,
    val totalTimeFormatted: String
)
