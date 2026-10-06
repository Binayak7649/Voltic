package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.*
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    WELCOME,
    PHONE_LOGIN,
    OTP_VERIFY,
    PROFILE_SETUP,
    MAIN,
    CHARGER_DETAILS,
    ROUTE_PLANNER,
    LIVE_ROUTE,
    CHARGING_SESSION,
    TRIP_ESTIMATOR,
    ADMIN_DASHBOARD,
    SAVED_LOCATIONS,
    NOTIFICATIONS,
    QR_SCANNER,
    CHARGER_VERIFICATION,
    CHARGING_RECEIPT,
    CHARGING_HISTORY
}

enum class MainTab {
    HOME,
    MAP,
    TRIPS,
    PROFILE
}

class VoltEliteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VoltEliteDatabase.getInstance(application)
    val repository = UnifiedChargingRepository(application, database)

    // Navigation & Screen Stack
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Auth State
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _phoneNumber = MutableStateFlow("9876543210")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("749216")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _otpCooldownSeconds = MutableStateFlow(45)
    val otpCooldownSeconds: StateFlow<Int> = _otpCooldownSeconds.asStateFlow()

    private val _isOtpVerifying = MutableStateFlow(false)
    val isOtpVerifying: StateFlow<Boolean> = _isOtpVerifying.asStateFlow()

    private var otpTimerJob: Job? = null

    // User Profile & Vehicle State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val vehicles: StateFlow<List<VehicleEntity>> = repository.vehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<TripEntity>> = repository.trips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLocations: StateFlow<List<SavedLocationEntity>> = repository.savedLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedIds: StateFlow<List<String>> = repository.bookmarkedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStations: StateFlow<List<ChargingStation>> = repository.allStations
    val activeSession: StateFlow<ChargingSession?> = repository.activeSession

    // Selected Station for Details
    private val _selectedStation = MutableStateFlow<ChargingStation?>(null)
    val selectedStation: StateFlow<ChargingStation?> = _selectedStation.asStateFlow()

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNetworkFilter = MutableStateFlow<ChargingNetwork?>(null)
    val selectedNetworkFilter: StateFlow<ChargingNetwork?> = _selectedNetworkFilter.asStateFlow()

    private val _onlyFastChargers = MutableStateFlow(false)
    val onlyFastChargers: StateFlow<Boolean> = _onlyFastChargers.asStateFlow()

    private val _selectedConnectorFilter = MutableStateFlow<ConnectorType?>(null)
    val selectedConnectorFilter: StateFlow<ConnectorType?> = _selectedConnectorFilter.asStateFlow()

    // Route Planner State
    private val _routeOrigin = MutableStateFlow("Indore, Madhya Pradesh")
    val routeOrigin: StateFlow<String> = _routeOrigin.asStateFlow()

    private val _routeDestination = MutableStateFlow("Bhopal, Madhya Pradesh")
    val routeDestination: StateFlow<String> = _routeDestination.asStateFlow()

    private val _selectedVehicleModel = MutableStateFlow("Tata Nexon EV")
    val selectedVehicleModel: StateFlow<String> = _selectedVehicleModel.asStateFlow()

    private val _currentRangeKm = MutableStateFlow(180)
    val currentRangeKm: StateFlow<Int> = _currentRangeKm.asStateFlow()

    private val _routePlan = MutableStateFlow<RoutePlan?>(null)
    val routePlan: StateFlow<RoutePlan?> = _routePlan.asStateFlow()

    // Trip Estimator State
    private val _estimatorDistanceKm = MutableStateFlow(250)
    val estimatorDistanceKm: StateFlow<Int> = _estimatorDistanceKm.asStateFlow()

    private val _estimatorRangeKm = MutableStateFlow(180)
    val estimatorRangeKm: StateFlow<Int> = _estimatorRangeKm.asStateFlow()

    private val _tripEstimate = MutableStateFlow(
        repository.estimateTrip(250, 180, 68)
    )
    val tripEstimate: StateFlow<TripEstimateResult> = _tripEstimate.asStateFlow()

    // --- QR Charging Integration State ---
    val qrService = com.example.data.service.QRChargingService()

    private val _chargerVerification = MutableStateFlow<ChargerVerificationResult?>(null)
    val chargerVerification: StateFlow<ChargerVerificationResult?> = _chargerVerification.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.UPI)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    private val _latestReceipt = MutableStateFlow<ChargingReceipt?>(null)
    val latestReceipt: StateFlow<ChargingReceipt?> = _latestReceipt.asStateFlow()

    private val _qrErrorMessage = MutableStateFlow<String?>(null)
    val qrErrorMessage: StateFlow<String?> = _qrErrorMessage.asStateFlow()

    val pastSessions: StateFlow<List<ChargingSessionEntity>> = repository.pastSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial splash timer
        viewModelScope.launch {
            delay(1600)
            if (_currentScreen.value == Screen.SPLASH) {
                _currentScreen.value = Screen.WELCOME
            }
        }

        // Default initial selected station (Tata Power)
        viewModelScope.launch {
            allStations.collect { list ->
                if (_selectedStation.value == null && list.isNotEmpty()) {
                    _selectedStation.value = list.first()
                }
            }
        }
    }

    // Filtered Stations computation
    val filteredStations: StateFlow<List<ChargingStation>> = combine(
        allStations,
        _searchQuery,
        _selectedNetworkFilter,
        _onlyFastChargers,
        _selectedConnectorFilter
    ) { stations, query, network, fastOnly, connector ->
        stations.filter { station ->
            val matchesQuery = query.isBlank() ||
                    station.name.contains(query, ignoreCase = true) ||
                    station.address.contains(query, ignoreCase = true) ||
                    station.network.displayName.contains(query, ignoreCase = true) ||
                    station.city.contains(query, ignoreCase = true)

            val matchesNetwork = network == null || station.network == network
            val matchesFast = !fastOnly || station.maxPowerKw >= 50
            val matchesConnector = connector == null || station.connectors.any { it.type == connector }

            matchesQuery && matchesNetwork && matchesFast && matchesConnector
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        _currentScreen.value = Screen.MAIN
    }

    fun selectStation(station: ChargingStation) {
        _selectedStation.value = station
        navigateTo(Screen.CHARGER_DETAILS)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleNetworkFilter(network: ChargingNetwork) {
        _selectedNetworkFilter.value = if (_selectedNetworkFilter.value == network) null else network
    }

    fun toggleFastChargers() {
        _onlyFastChargers.value = !_onlyFastChargers.value
    }

    fun setConnectorFilter(connector: ConnectorType?) {
        _selectedConnectorFilter.value = connector
    }

    fun toggleBookmark(stationId: String) {
        viewModelScope.launch {
            val isBookmarked = bookmarkedIds.value.contains(stationId)
            repository.toggleBookmark(stationId, isBookmarked)
        }
    }

    // Auth methods
    fun startPhoneAuth(phone: String) {
        _phoneNumber.value = phone
        startOtpCooldown()
        navigateTo(Screen.OTP_VERIFY)
    }

    fun resendOtp() {
        startOtpCooldown()
    }

    private fun startOtpCooldown() {
        otpTimerJob?.cancel()
        _otpCooldownSeconds.value = 45
        otpTimerJob = viewModelScope.launch {
            while (_otpCooldownSeconds.value > 0) {
                delay(1000)
                _otpCooldownSeconds.value -= 1
            }
        }
    }

    fun verifyOtp(enteredCode: String) {
        loginWithBackend("demo@voltelite.app", "Password123!")
    }

    fun loginWithGoogle() {
        loginWithBackend("demo@voltelite.app", "Password123!")
    }

    fun exploreAsGuest() {
        loginWithBackend("demo@voltelite.app", "Password123!")
    }

    fun loginWithBackend(email: String = "demo@voltelite.app", pass: String = "Password123!") {
        viewModelScope.launch {
            _isOtpVerifying.value = true
            try {
                val api = com.example.data.network.VoltEliteApiClient.getService()
                val res = api.login(com.example.data.network.LoginRequestDto(email, pass))
                if (res.isSuccessful && res.body()?.data != null) {
                    val tokenData = res.body()!!.data!!
                    com.example.data.network.VoltEliteApiClient.authToken = tokenData.accessToken
                }
            } catch (e: Exception) {
                // Ignore if offline
            }
            _isOtpVerifying.value = false
            _isAuthenticated.value = true
            _userProfile.value = UserProfile(
                name = "Binayak Tiwari",
                email = email,
                isGoogleLinked = true
            )
            repository.syncChargingHistoryFromBackend()
            navigateTo(Screen.MAIN)
        }
    }

    fun logout() {
        _isAuthenticated.value = false
        com.example.data.network.VoltEliteApiClient.authToken = null
        navigateTo(Screen.WELCOME)
    }

    // Route Planner Actions
    fun setRouteEndpoints(origin: String, dest: String) {
        _routeOrigin.value = origin
        _routeDestination.value = dest
    }

    fun setCurrentRangeKm(range: Int) {
        _currentRangeKm.value = range
    }

    fun calculateRoute() {
        _routePlan.value = repository.calculateRoute(
            origin = _routeOrigin.value,
            destination = _routeDestination.value,
            currentRangeKm = _currentRangeKm.value,
            evModel = _selectedVehicleModel.value
        )
    }

    // Estimator Actions
    fun updateEstimator(distance: Int, range: Int) {
        _estimatorDistanceKm.value = distance
        _estimatorRangeKm.value = range
        _tripEstimate.value = repository.estimateTrip(distance, range, 68)
    }

    // Charging Session Actions
    fun startCharging(station: ChargingStation) {
        val connector = station.connectors.firstOrNull { it.status == StationStatus.AVAILABLE }
            ?: station.connectors.first()
        repository.startChargingSession(station, connector)
        navigateTo(Screen.CHARGING_SESSION)
    }

    fun stopCharging() {
        viewModelScope.launch {
            repository.stopChargingSession()
        }
    }

    fun deleteSavedLocation(id: String) {
        viewModelScope.launch {
            database.savedLocationDao().deleteLocation(id)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            database.notificationDao().markAsRead(id)
        }
    }

    // --- QR Charging Actions ---
    fun startQrScan() {
        _qrErrorMessage.value = null
        navigateTo(Screen.QR_SCANNER)
    }

    fun dismissQrError() {
        _qrErrorMessage.value = null
    }

    fun processQrCode(payload: String) {
        _qrErrorMessage.value = null
        viewModelScope.launch {
            try {
                val api = com.example.data.network.VoltEliteApiClient.getService()
                val response = api.verifyQR(
                    com.example.data.network.QRVerifyRequestDto(
                        qrPayload = payload,
                        vehicleMakeModel = "Tata Nexon EV",
                        vehicleConnector = "CCS 2"
                    )
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()!!.data!!
                    val network = when (data.operator.lowercase()) {
                        "chargezone" -> ChargingNetwork.CHARGE_ZONE
                        "statiq" -> ChargingNetwork.STATIQ
                        "jio-bp" -> ChargingNetwork.JIO_BP
                        "zeon", "zeon charging" -> ChargingNetwork.ZEON
                        "bpcl", "bpcl edrive" -> ChargingNetwork.BPCL
                        "kazam" -> ChargingNetwork.KAZAM
                        "ather grid", "ather" -> ChargingNetwork.ATHER_GRID
                        else -> ChargingNetwork.TATA_POWER
                    }
                    val connType = if (data.connectorType.contains("Type 2", ignoreCase = true)) ConnectorType.TYPE_2 else ConnectorType.CCS_2
                    val parsed = ParsedQrCharger(
                        provider = network,
                        stationId = data.stationId,
                        stationName = data.stationName,
                        evseId = data.evseId,
                        connectorId = data.chargerId,
                        connectorType = connType,
                        powerKw = data.powerKw,
                        tariffPerKwh = data.tariffPerKwh,
                        location = data.location,
                        status = if (data.status == "AVAILABLE") StationStatus.AVAILABLE else StationStatus.BUSY,
                        supportsRemoteStart = data.supportsRemoteStart,
                        rawPayload = payload
                    )
                    val compat = VehicleCompatibility(
                        isCompatible = data.compatibility.isCompatible,
                        userVehicleMakeModel = data.compatibility.userVehicleMakeModel,
                        userVehicleConnector = ConnectorType.CCS_2,
                        chargerConnector = connType,
                        message = data.compatibility.message
                    )
                    _chargerVerification.value = ChargerVerificationResult(
                        charger = parsed,
                        compatibility = compat,
                        estimatedFullCost = data.estimatedFullCost,
                        estimatedDurationMin = data.estimatedDurationMin
                    )
                    navigateTo(Screen.CHARGER_VERIFICATION)
                    return@launch
                }
            } catch (e: Exception) {
                // Offline fallback
            }

            // Local fallback parser
            val parseResult = qrService.parseQr(payload)
            parseResult.onSuccess { parsed ->
                val defaultVehicle = EvVehicle(
                    id = "veh_01",
                    make = "Tata",
                    model = "Nexon EV",
                    batteryCapacityKwh = 40.5,
                    realWorldRangeKm = 312,
                    connectorType = ConnectorType.CCS_2,
                    currentBatteryPercent = 68,
                    isDefault = true
                )
                val verification = qrService.verifyCharger(parsed, defaultVehicle)
                _chargerVerification.value = verification
                navigateTo(Screen.CHARGER_VERIFICATION)
            }.onFailure { error ->
                _qrErrorMessage.value = error.message ?: "Unable to read QR code. Please try again or enter Charger ID manually."
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun confirmStartChargingFromQr() {
        val verification = _chargerVerification.value ?: return
        val charger = verification.charger
        repository.startChargingSessionFromQr(charger, _selectedPaymentMethod.value)
        navigateTo(Screen.CHARGING_SESSION)
    }

    fun stopChargingAndShowReceipt() {
        viewModelScope.launch {
            val receipt = repository.stopChargingSession()
            if (receipt != null) {
                _latestReceipt.value = receipt
                navigateTo(Screen.CHARGING_RECEIPT)
            } else {
                navigateTo(Screen.MAIN)
            }
        }
    }

    fun showReceiptFromHistory(session: ChargingSessionEntity) {
        val receipt = ChargingReceipt(
            sessionId = session.id,
            txnId = session.txnId,
            stationName = session.stationName,
            evseId = session.evseId,
            network = when {
                session.networkName.contains("ChargeZone", ignoreCase = true) -> ChargingNetwork.CHARGE_ZONE
                session.networkName.contains("Statiq", ignoreCase = true) -> ChargingNetwork.STATIQ
                session.networkName.contains("Jio", ignoreCase = true) -> ChargingNetwork.JIO_BP
                session.networkName.contains("Zeon", ignoreCase = true) -> ChargingNetwork.ZEON
                session.networkName.contains("BPCL", ignoreCase = true) -> ChargingNetwork.BPCL
                else -> ChargingNetwork.TATA_POWER
            },
            connectorType = ConnectorType.CCS_2,
            powerKw = session.powerKw,
            energyDeliveredKwh = session.energyAddedKwh,
            chargingTimeMinutes = session.durationMin,
            totalAmountRupees = session.totalCostRupees,
            tariffPerKwh = session.tariffPerKwh,
            paymentMethod = session.paymentMethod,
            timestampFormatted = "03 Oct 2026",
            invoiceNumber = "INV-${session.id.takeLast(6).uppercase()}"
        )
        _latestReceipt.value = receipt
        navigateTo(Screen.CHARGING_RECEIPT)
    }
}
