package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChargingNetwork
import com.example.model.ConnectorType
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.components.CircularChargingGauge
import com.example.ui.theme.*

@Composable
fun ChargingSessionScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsState()

    val currentPercent = activeSession?.currentPercent ?: 68f
    val energyAdded = activeSession?.energyAddedKwh ?: 24.8
    val timeLeft = activeSession?.estMinutesRemaining ?: 18
    val totalCost = activeSession?.totalCostRupees ?: 446.0
    val stationName = activeSession?.stationName ?: "Tata Power Charging Station"
    val powerKw = activeSession?.powerKw ?: 60

    var showStopDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }

                Text(
                    text = "Charging in Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )

                IconButton(onClick = { viewModel.startQrScan() }) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = VoltGreen)
                }
            }
        }

        // Circular Glowing Gauge
        item {
            Spacer(modifier = Modifier.height(10.dp))
            CircularChargingGauge(
                currentPercent = currentPercent,
                targetPercent = 85f,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        // Futuristic EV Vehicle Visual
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.futuristic_ev_car),
                        contentDescription = "EV Car",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.6f to VoltDarkBg.copy(alpha = 0.5f),
                                    1.0f to VoltDarkBg.copy(alpha = 0.9f)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen)
                        ) {
                            Text(
                                text = "Tata Nexon EV • Connected",
                                color = VoltGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Station Info Subheading
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stationName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "CCS 2 • $powerKw kW (Fast Charger)",
                    fontSize = 13.sp,
                    color = VoltCyan
                )
            }
        }

        // 3 Telemetry Info Cards (Added Energy, Est Time Left, Total Cost)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    title = "Added Energy",
                    value = "${energyAdded} kWh",
                    valueColor = VoltTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "Est. Time Left",
                    value = "$timeLeft min",
                    valueColor = VoltCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "Total Cost",
                    value = "₹ ${totalCost.toInt()}",
                    valueColor = VoltGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stop Charging Button
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showStopDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("stop_charging_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VoltRed),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = VoltTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stop Charging",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }
        }

        // Live Session Waveform Status
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VoltCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(VoltGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Telemetry Feed (OCPI 2.2)",
                            fontSize = 12.sp,
                            color = VoltTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Graph",
                        tint = VoltCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = VoltCard,
            icon = {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Stop", tint = VoltRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Stop Charging Session?", fontWeight = FontWeight.Bold, color = VoltTextPrimary)
            },
            text = {
                Column {
                    Text("Are you sure you want to end your charging session?", fontSize = 14.sp, color = VoltTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VoltCardElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivered Energy:", fontSize = 12.sp, color = VoltTextSecondary)
                                Text("${energyAdded} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Amount:", fontSize = 12.sp, color = VoltTextSecondary)
                                Text("₹${totalCost.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = VoltGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        viewModel.stopChargingAndShowReceipt()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_stop_button")
                ) {
                    Text("Stop Charging", color = VoltTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel", color = VoltTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = VoltCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = VoltTextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}
