package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingNetwork
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun TripEstimatorScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val distance by viewModel.estimatorDistanceKm.collectAsState()
    val range by viewModel.estimatorRangeKm.collectAsState()
    val estimate by viewModel.tripEstimate.collectAsState()
    val evModel by viewModel.selectedVehicleModel.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trip Estimator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = VoltDarkBg
            ) {
                Button(
                    onClick = {
                        viewModel.setRouteEndpoints("Indore, Madhya Pradesh", "Bhopal, Madhya Pradesh")
                        viewModel.calculateRoute()
                        viewModel.navigateTo(Screen.ROUTE_PLANNER)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                        .testTag("start_plan_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Start Plan",
                        color = VoltDarkBg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // EV Model selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "EV", tint = VoltCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("EV Model", fontSize = 11.sp, color = VoltTextSecondary)
                            Text(evModel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                        }
                    }
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = VoltTextSecondary)
                }
            }

            // Distance selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distance", fontSize = 13.sp, color = VoltTextSecondary)
                        Text("$distance km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                    }
                    Slider(
                        value = distance.toFloat(),
                        onValueChange = { viewModel.updateEstimator(it.toInt(), range) },
                        valueRange = 50f..800f,
                        colors = SliderDefaults.colors(thumbColor = VoltCyan, activeTrackColor = VoltCyan)
                    )
                }
            }

            // Battery Range selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Battery Range", fontSize = 13.sp, color = VoltTextSecondary)
                        Text("$range km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VoltGreen)
                    }
                    Slider(
                        value = range.toFloat(),
                        onValueChange = { viewModel.updateEstimator(distance, it.toInt()) },
                        valueRange = 80f..500f,
                        colors = SliderDefaults.colors(thumbColor = VoltGreen, activeTrackColor = VoltGreen)
                    )
                }
            }

            // Estimated Charging Stops
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated Charging Stops", fontSize = 12.sp, color = VoltTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = "Stops", tint = VoltYellow)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${estimate.stopsCount} stops",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VoltTextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Charging Time", fontSize = 11.sp, color = VoltTextSecondary)
                            Text(estimate.chargingTimeFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VoltCyan)
                        }
                    }
                }
            }

            // Estimated Cost
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated Cost", fontSize = 12.sp, color = VoltTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = estimate.costRangeFormatted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VoltGreen
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAnalyticsScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Admin & Analytics Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            item {
                Text("Platform Telemetry & Metrics", fontSize = 14.sp, color = VoltTextSecondary)
            }

            // KPIs Grid
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricBox(title = "Total Users", value = "14,250", sub = "+12% this week", color = VoltCyan, modifier = Modifier.weight(1f))
                    MetricBox(title = "Active Sessions", value = "38 Live", sub = "Across 8 networks", color = VoltGreen, modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricBox(title = "Online Stations", value = "482 / 510", sub = "94.5% Uptime", color = VoltGreen, modifier = Modifier.weight(1f))
                    MetricBox(title = "Total Revenue", value = "₹ 1.84M", sub = "Current Month", color = VoltYellow, modifier = Modifier.weight(1f))
                }
            }

            // Network Distribution Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Network Share (Roaming Interconnects)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        NetworkShareBar("Tata Power (EZ Charge)", 0.34f, VoltCyan, "34%")
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkShareBar("ChargeZone Direct", 0.24f, VoltGreen, "24%")
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkShareBar("Statiq Open OCPI", 0.18f, VoltYellow, "18%")
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkShareBar("Jio-bp pulse", 0.14f, VoltPurple, "14%")
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkShareBar("Zeon / BPCL / Kazam / Ather", 0.10f, VoltRed, "10%")
                    }
                }
            }

            // Roaming status
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Protocol Adapters Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• OCPI 2.2.1 Gateway: Connected (Latency: 28ms)", fontSize = 12.sp, color = VoltGreen)
                        Text("• OCPP 2.0.1 Direct Stream: Active", fontSize = 12.sp, color = VoltGreen)
                        Text("• Real-time Availability Poll: Every 15s", fontSize = 12.sp, color = VoltCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun SavedLocationsScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val savedLocations by viewModel.savedLocations.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saved Locations", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(savedLocations, key = { it.id }) { loc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(VoltCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = loc.label, tint = VoltCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(loc.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VoltTextPrimary)
                                Text(loc.address, fontSize = 12.sp, color = VoltTextSecondary)
                            }
                        }

                        IconButton(onClick = { viewModel.deleteSavedLocation(loc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VoltTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp))
                        .clickable { viewModel.markNotificationRead(notif.id) },
                    colors = CardDefaults.cardColors(containerColor = if (notif.isRead) VoltCard else VoltCardElevated)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VoltGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Alert", tint = VoltGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VoltTextPrimary)
                                Text(notif.timeAgo, fontSize = 11.sp, color = VoltTextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.message, fontSize = 12.sp, color = VoltTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = VoltCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = VoltTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, fontSize = 10.sp, color = VoltTextMuted)
        }
    }
}

@Composable
private fun NetworkShareBar(name: String, fraction: Float, color: Color, percent: String) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 12.sp, color = VoltTextPrimary)
            Text(percent, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = VoltCardElevated,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
        )
    }
}
