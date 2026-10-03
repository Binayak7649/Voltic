package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingStation
import com.example.model.StationStatus
import com.example.ui.theme.*

@Composable
fun InteractiveChargerMap(
    stations: List<ChargingStation>,
    selectedStation: ChargingStation?,
    onStationSelect: (ChargingStation) -> Unit,
    onNavigateClick: (ChargingStation) -> Unit,
    onDetailsClick: (ChargingStation) -> Unit,
    onScanQrClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }
    var isSatelliteView by remember { mutableStateOf(false) }

    // Pulse animation for user location radar
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
        modifier = modifier
            .fillMaxSize()
            .background(if (isSatelliteView) Color(0xFF071217) else VoltDarkBg)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffsetX += dragAmount.x
                    panOffsetY += dragAmount.y
                }
            }
    ) {
        // High-tech vector futuristic map background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base road network grid lines
            val roadBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF141C2E), Color(0xFF1B263E))
            )
            val highwayBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF223150), Color(0xFF2E426C))
            )

            // Arterials & Highways
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
            drawLine(
                brush = roadBrush,
                start = Offset(width * 0.65f + panOffsetX * 0.3f, 0f),
                end = Offset(width * 0.25f + panOffsetX * 0.3f, height),
                strokeWidth = 4f
            )

            // Neon route connector to selected station if available
            val userCenter = Offset(width * 0.5f + panOffsetX, height * 0.45f + panOffsetY)

            // Draw user location radar pulse
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
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = userCenter
            )
        }

        // City labels overlay
        Box(modifier = Modifier.fillMaxSize()) {
            CityTag(name = "Vijay Nagar", x = 110f + panOffsetX * 0.7f, y = 220f + panOffsetY * 0.7f)
            CityTag(name = "Palasia", x = 190f + panOffsetX * 0.7f, y = 370f + panOffsetY * 0.7f)
            CityTag(name = "Indore Center", x = 160f + panOffsetX * 0.7f, y = 490f + panOffsetY * 0.7f, isCenter = true)
            CityTag(name = "Bhawarkuan", x = 80f + panOffsetX * 0.7f, y = 630f + panOffsetY * 0.7f)
        }

        // Station Markers on the Map
        stations.forEachIndexed { index, station ->
            val markerOffset = calculateStationOffset(index, station, panOffsetX, panOffsetY)
            val isSelected = selectedStation?.id == station.id

            Box(
                modifier = Modifier
                    .offset(x = markerOffset.first.dp, y = markerOffset.second.dp)
                    .clickable { onStationSelect(station) }
                    .testTag("map_marker_${station.id}"),
                contentAlignment = Alignment.Center
            ) {
                MapStationPin(station = station, isSelected = isSelected)
            }
        }

        // Top Floating Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .statusBarsPadding()
                .clip(RoundedCornerShape(24.dp)),
            color = VoltCard.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = VoltGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search this area",
                    color = VoltTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = VoltCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Floating Action Buttons on Right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = { onScanQrClick?.invoke() },
                containerColor = VoltGreen,
                contentColor = VoltDarkBg,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("map_scan_qr_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(22.dp))
            }

            FloatingActionButton(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                },
                containerColor = VoltCard,
                contentColor = VoltCyan,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = { isSatelliteView = !isSatelliteView },
                containerColor = VoltCard,
                contentColor = if (isSatelliteView) VoltGreen else VoltTextSecondary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Map Style", modifier = Modifier.size(20.dp))
            }
        }

        // Bottom Selected Station Floating Card (Matching Screenshot exactly!)
        selectedStation?.let { station ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(20.dp))
                    .clickable { onDetailsClick(station) },
                colors = CardDefaults.cardColors(containerColor = VoltCard.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VoltTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 ${station.distanceKm} km • ${station.etaMinutes} min • Near ${station.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = VoltTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltYellow.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "★ ${station.rating} (${station.reviewsCount})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltYellow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Available ${station.totalAvailable}/${station.totalPorts}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Text(
                                text = "Fast (${station.maxPowerKw} kW)",
                                fontSize = 12.sp,
                                color = VoltCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onNavigateClick(station) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("map_navigate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            tint = VoltDarkBg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Navigate",
                            color = VoltDarkBg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapStationPin(
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
                .size(if (isSelected) 44.dp else 36.dp)
                .clip(CircleShape)
                .background(pinColor)
                .border(2.dp, if (isSelected) Color.White else VoltDarkBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = station.name,
                tint = VoltDarkBg,
                modifier = Modifier.size(if (isSelected) 26.dp else 20.dp)
            )
        }

        // Pointer triangle
        Canvas(modifier = Modifier.size(8.dp, 6.dp)) {
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
private fun CityTag(name: String, x: Float, y: Float, isCenter: Boolean = false) {
    Text(
        text = name,
        color = if (isCenter) VoltCyan.copy(alpha = 0.9f) else VoltTextSecondary.copy(alpha = 0.45f),
        fontSize = if (isCenter) 16.sp else 12.sp,
        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.offset(x = x.dp, y = y.dp)
    )
}

private fun calculateStationOffset(index: Int, station: ChargingStation, panX: Float, panY: Float): Pair<Float, Float> {
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
