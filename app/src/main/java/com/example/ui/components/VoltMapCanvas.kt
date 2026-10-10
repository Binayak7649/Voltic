package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ChargingStation
import com.example.model.ConnectorType
import com.example.model.StationStatus
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

// Dark neon theme JSON for Google Maps matching VoltElite UI
private const val GOOGLE_MAPS_DARK_STYLE_JSON = """[
  {"elementType": "geometry", "stylers": [{"color": "#121624"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#121624"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8da2b5"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#00E676"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#00E5FF"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#0b1d24"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#1b2438"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#131a29"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#283754"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#060b12"}]}
]"""

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun InteractiveChargerMap(
    stations: List<ChargingStation>,
    selectedStation: ChargingStation?,
    userLocation: Pair<Double, Double>? = null,
    carsOnlyFilter: Boolean = true,
    onCarsOnlyToggle: (Boolean) -> Unit = {},
    onRefreshLocation: (Double, Double) -> Unit = { _, _ -> },
    onStationSelect: (ChargingStation) -> Unit,
    onNavigateClick: (ChargingStation) -> Unit,
    onDetailsClick: (ChargingStation) -> Unit,
    onScanQrClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGoogleMapMode by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyFastStations by remember { mutableStateOf(false) }

    val mapsApiKey = remember {
        com.example.BuildConfig.GOOGLE_MAPS_PLATFORM_KEY.ifBlank {
            com.example.BuildConfig.GOOGLE_MAPS_API_KEY
        }
    }

    val defaultLat = userLocation?.first ?: selectedStation?.latitude ?: 22.7196
    val defaultLon = userLocation?.second ?: selectedStation?.longitude ?: 75.8577

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLon), 13f)
    }

    // Move camera when selected station changes
    LaunchedEffect(selectedStation) {
        selectedStation?.let { station ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(station.latitude, station.longitude),
                    14.5f
                ),
                durationMs = 800
            )
        }
    }

    // Permission tracking
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            onRefreshLocation(loc.latitude, loc.longitude)
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        14f
                                    )
                                )
                            }
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    // Filter displayed stations
    val displayedStations = remember(stations, searchQuery, onlyFastStations) {
        stations.filter { st ->
            val matchesQuery = searchQuery.isBlank() ||
                    st.name.contains(searchQuery, ignoreCase = true) ||
                    st.address.contains(searchQuery, ignoreCase = true) ||
                    st.network.displayName.contains(searchQuery, ignoreCase = true)
            val matchesFast = !onlyFastStations || st.maxPowerKw >= 50
            matchesQuery && matchesFast
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        if (isGoogleMapMode) {
            // --- Live Google Maps Integration ---
            val mapProperties = remember(hasLocationPermission) {
                MapProperties(
                    mapStyleOptions = MapStyleOptions(GOOGLE_MAPS_DARK_STYLE_JSON),
                    isMyLocationEnabled = hasLocationPermission
                )
            }
            val mapUiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true
                )
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_map_view"),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = {
                    // Deselect or dismiss
                }
            ) {
                displayedStations.forEach { station ->
                    val isSelected = selectedStation?.id == station.id
                    val markerState = rememberMarkerState(
                        key = station.id,
                        position = LatLng(station.latitude, station.longitude)
                    )

                    MarkerComposable(
                        keys = arrayOf<Any>(station.id, isSelected),
                        state = markerState,
                        onClick = {
                            onStationSelect(station)
                            true
                        }
                    ) {

                        MapStationPin(
                            station = station,
                            isSelected = isSelected
                        )
                    }
                }
            }
        } else {
            // --- Futuristic Vector Radar Map Fallback ---
            FuturisticRadarMapCanvas(
                stations = displayedStations,
                selectedStation = selectedStation,
                onStationSelect = onStationSelect
            )
        }

        // --- Top Bar with Search & Filter Chips ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search & Mode Switch Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = VoltCard.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = VoltGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search EV car chargers in area...",
                                color = VoltTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = VoltTextPrimary,
                            unfocusedTextColor = VoltTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_input")
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = VoltTextSecondary)
                        }
                    }

                    // Mode Switch Toggle (Google Map / Radar)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGoogleMapMode) VoltGreen.copy(alpha = 0.2f) else VoltCardElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGoogleMapMode) VoltGreen else VoltCardBorder
                        ),
                        modifier = Modifier
                            .clickable { isGoogleMapMode = !isGoogleMapMode }
                            .padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isGoogleMapMode) Icons.Default.Map else Icons.Default.Radar,
                                contentDescription = "Map Style",
                                tint = if (isGoogleMapMode) VoltGreen else VoltCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGoogleMapMode) "Google Map" else "Radar Grid",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips (Car Only + Fast Charger)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cars Only Chip (Crucial feature)
                FilterChip(
                    selected = carsOnlyFilter,
                    onClick = { onCarsOnlyToggle(!carsOnlyFilter) },
                    label = {
                        Text(
                            text = "🚗 Cars Only (4W)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (carsOnlyFilter) VoltGreen else VoltTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car Chargers",
                            tint = if (carsOnlyFilter) VoltGreen else VoltTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VoltCard.copy(alpha = 0.9f),
                        selectedContainerColor = VoltGreen.copy(alpha = 0.2f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = carsOnlyFilter,
                        borderColor = VoltCardBorder,
                        selectedBorderColor = VoltGreen
                    )
                )

                // Fast Chargers Chip
                FilterChip(
                    selected = onlyFastStations,
                    onClick = { onlyFastStations = !onlyFastStations },
                    label = {
                        Text(
                            text = "⚡ Fast (≥50 kW)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (onlyFastStations) VoltCyan else VoltTextSecondary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VoltCard.copy(alpha = 0.9f),
                        selectedContainerColor = VoltCyan.copy(alpha = 0.2f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = onlyFastStations,
                        borderColor = VoltCardBorder,
                        selectedBorderColor = VoltCyan
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Station count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VoltCard.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                ) {
                    Text(
                        text = "${displayedStations.size} Car Stations",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                // Maps Platform Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (mapsApiKey.isNotBlank()) VoltGreen.copy(alpha = 0.15f) else VoltCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (mapsApiKey.isNotBlank()) VoltGreen.copy(alpha = 0.6f) else VoltCyan.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (mapsApiKey.isNotBlank()) VoltGreen else VoltCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (mapsApiKey.isNotBlank()) "Maps Live" else "AI Studio Maps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mapsApiKey.isNotBlank()) VoltGreen else VoltCyan
                        )
                    }
                }
            }
        }

        // --- Floating Action Buttons (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // My Location FAB
            FloatingActionButton(
                onClick = {
                    if (hasLocationPermission) {
                        try {
                            val fused = LocationServices.getFusedLocationProviderClient(context)
                            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        onRefreshLocation(loc.latitude, loc.longitude)
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(loc.latitude, loc.longitude),
                                                    14.5f
                                                )
                                            )
                                        }
                                    }
                                }
                        } catch (_: SecurityException) {}
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .testTag("map_my_location_button"),
                containerColor = VoltCard,
                contentColor = VoltGreen,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Quick QR Scan FAB on map
            if (onScanQrClick != null) {
                FloatingActionButton(
                    onClick = onScanQrClick,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("map_quick_scan_qr_fab"),
                    containerColor = VoltGreen,
                    contentColor = VoltDarkBg,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Zoom In
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                containerColor = VoltCard,
                contentColor = VoltTextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                containerColor = VoltCard,
                contentColor = VoltTextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }
        }

        // --- Bottom Station Preview Card ---
        selectedStation?.let { station ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(22.dp))
                    .testTag("map_selected_station_card"),
                colors = CardDefaults.cardColors(containerColor = VoltCard.copy(alpha = 0.98f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = station.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(station.network.brandColorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = station.network.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(station.network.brandColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${station.distanceKm} km away • ${station.etaMinutes} mins drive",
                                fontSize = 12.sp,
                                color = VoltTextSecondary
                            )
                        }

                        // Rating Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = VoltYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = station.rating.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Car Compatibility & Availability Specs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🚗 ${station.totalAvailable}/${station.totalPorts} Car Ports",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⚡ ${station.maxPowerKw} kW DC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Text(
                                text = "₹${station.pricePerKwh}/kWh",
                                fontSize = 11.sp,
                                color = VoltTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons (Navigate, Details, Scan QR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Navigate Button
                        Button(
                            onClick = {
                                try {
                                    // Launch external Google Navigation intent if available, and also navigate inside app
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    }
                                } catch (_: Exception) {}
                                onNavigateClick(station)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("map_navigate_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navigate",
                                tint = VoltDarkBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Navigate",
                                color = VoltDarkBg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // View Details Button
                        OutlinedButton(
                            onClick = { onDetailsClick(station) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("map_station_details_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltTextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                        ) {
                            Text("Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Scan QR Button
                        if (onScanQrClick != null) {
                            Button(
                                onClick = onScanQrClick,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(46.dp)
                                    .testTag("map_station_scan_qr_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = VoltCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = VoltDarkBg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scan QR",
                                    color = VoltDarkBg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapStationPin(
    station: ChargingStation,
    isSelected: Boolean
) {
    val pinColor = when {
        station.maxPowerKw >= 100 -> VoltPurple
        station.status == StationStatus.AVAILABLE -> VoltGreen
        station.status == StationStatus.BUSY -> VoltYellow
        else -> VoltRed
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 46.dp else 38.dp)
                .clip(CircleShape)
                .background(pinColor)
                .border(2.dp, if (isSelected) Color.White else VoltDarkBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = station.name,
                tint = VoltDarkBg,
                modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
            )
        }

        // Pointer triangle
        Canvas(modifier = Modifier.size(10.dp, 8.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(path, pinColor)
        }
    }
}

@Composable
private fun FuturisticRadarMapCanvas(
    stations: List<ChargingStation>,
    selectedStation: ChargingStation?,
    onStationSelect: (ChargingStation) -> Unit
) {
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 44f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffsetX += dragAmount.x
                    panOffsetY += dragAmount.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val roadBrush = Brush.linearGradient(listOf(Color(0xFF141C2E), Color(0xFF1B263E)))
            val highwayBrush = Brush.linearGradient(listOf(Color(0xFF223150), Color(0xFF2E426C)))

            drawLine(
                brush = highwayBrush,
                start = Offset(0f, height * 0.45f + panOffsetY * 0.5f),
                end = Offset(width, height * 0.55f + panOffsetY * 0.5f),
                strokeWidth = 10f
            )
            drawLine(
                brush = highwayBrush,
                start = Offset(width * 0.3f + panOffsetX * 0.5f, 0f),
                end = Offset(width * 0.7f + panOffsetX * 0.5f, height),
                strokeWidth = 8f
            )
            drawLine(
                brush = roadBrush,
                start = Offset(0f, height * 0.25f + panOffsetY * 0.3f),
                end = Offset(width, height * 0.30f + panOffsetY * 0.3f),
                strokeWidth = 4f
            )
            drawLine(
                brush = roadBrush,
                start = Offset(0f, height * 0.75f + panOffsetY * 0.3f),
                end = Offset(width, height * 0.70f + panOffsetY * 0.3f),
                strokeWidth = 4f
            )

            val userCenter = Offset(width * 0.5f + panOffsetX, height * 0.45f + panOffsetY)
            drawCircle(
                color = VoltCyan.copy(alpha = pulseAlpha),
                radius = pulseRadius * 2f,
                center = userCenter,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = VoltCyan,
                radius = 10f,
                center = userCenter
            )
        }

        stations.forEachIndexed { index, station ->
            val markerOffset = calculateStationOffset(index, panOffsetX, panOffsetY)
            val isSelected = selectedStation?.id == station.id

            Box(
                modifier = Modifier
                    .offset(x = markerOffset.first.dp, y = markerOffset.second.dp)
                    .clickable { onStationSelect(station) }
                    .testTag("radar_marker_${station.id}"),
                contentAlignment = Alignment.Center
            ) {
                MapStationPin(station = station, isSelected = isSelected)
            }
        }
    }
}

private fun calculateStationOffset(index: Int, panX: Float, panY: Float): Pair<Float, Float> {
    val baseX = when (index % 6) {
        0 -> 140f
        1 -> 240f
        2 -> 70f
        3 -> 270f
        4 -> 190f
        else -> 110f
    }
    val baseY = when (index % 6) {
        0 -> 180f
        1 -> 260f
        2 -> 310f
        3 -> 420f
        4 -> 540f
        else -> 600f
    }
    return Pair(baseX + panX * 0.25f, baseY + panY * 0.25f)
}
