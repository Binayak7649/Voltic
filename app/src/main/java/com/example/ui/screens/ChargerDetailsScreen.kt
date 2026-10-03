package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChargingStation
import com.example.model.StationStatus
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun ChargerDetailsScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val station = viewModel.selectedStation.collectAsState().value
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()

    if (station == null) {
        Box(
            modifier = modifier.fillMaxSize().background(VoltDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Station not found", color = VoltTextSecondary)
        }
        return
    }

    val isBookmarked = bookmarkedIds.contains(station.id)

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = VoltDarkBg,
                tonalElevation = 8.dp
            ) {
                Column {
                    HorizontalDivider(color = VoltCardBorder, thickness = 0.8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.LIVE_ROUTE) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("details_navigate_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, VoltCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltCyan)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = "Navigate", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.startQrScan() },
                            modifier = Modifier
                                .weight(1.8f)
                                .height(50.dp)
                                .testTag("details_scan_qr_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = VoltDarkBg, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan QR to Charge", color = VoltDarkBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
        ) {
            // 1. Station Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ev_station_banner),
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.5f to VoltDarkBg.copy(alpha = 0.4f),
                                    1.0f to VoltDarkBg
                                )
                            )
                    )

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.MAIN) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(VoltDarkBg.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleBookmark(station.id) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(VoltDarkBg.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = if (isBookmarked) VoltGreen else VoltTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Title & Ratings
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = station.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VoltTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "📍 ${station.distanceKm} km • ${station.etaMinutes} min • Near ${station.address}",
                        fontSize = 13.sp,
                        color = VoltTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltYellow.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "★ ${station.rating} (${station.reviewsCount} reviews)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltYellow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                        ) {
                            Text(
                                text = "Fast (${station.maxPowerKw} kW)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VoltCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Text(
                                text = "CCS 2",
                                fontSize = 12.sp,
                                color = VoltTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Text(
                                text = "Type 2",
                                fontSize = 12.sp,
                                color = VoltTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Available Connectors
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "Available Connectors",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        station.connectors.forEach { connector ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = VoltCard)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Power,
                                        contentDescription = connector.type.displayName,
                                        tint = if (connector.status == StationStatus.AVAILABLE) VoltGreen else VoltYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = connector.type.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltTextPrimary
                                    )
                                    Text(
                                        text = "${connector.powerKw} kW",
                                        fontSize = 11.sp,
                                        color = VoltCyan
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${connector.availableCount}/${connector.totalCount} Free",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (connector.status == StationStatus.AVAILABLE) VoltGreen else VoltYellow
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Pricing Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
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
                        Column {
                            Text(
                                text = "Charging Tariff",
                                fontSize = 12.sp,
                                color = VoltTextSecondary
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "₹ ${station.pricePerKwh.toInt()}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = VoltTextPrimary
                                )
                                Text(
                                    text = " / kWh",
                                    fontSize = 14.sp,
                                    color = VoltTextSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.startCharging(station) },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Start Charging",
                                color = VoltDarkBg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. Amenities
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "Station Amenities",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AmenityBadge(icon = Icons.Default.LocalParking, label = "Parking")
                        AmenityBadge(icon = Icons.Default.LocalCafe, label = "Cafeteria")
                        AmenityBadge(icon = Icons.Default.Wc, label = "Restroom")
                        AmenityBadge(icon = Icons.Default.Wifi, label = "Wi-Fi")
                        AmenityBadge(icon = Icons.Default.AccessTime, label = "24/7")
                    }
                }
            }

            // 6. Charger Operator Details Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(station.network.brandColorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = station.network.displayName,
                                    tint = Color(station.network.brandColorHex),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = station.network.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = VoltTextPrimary
                                )
                                Text(
                                    text = "OCPI 2.2 Roaming • Verified Partner",
                                    fontSize = 11.sp,
                                    color = VoltGreen
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                        ) {
                            Text("Visit", fontSize = 12.sp, color = VoltCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmenityBadge(
    icon: ImageVector,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(VoltCardElevated)
                .border(1.dp, VoltCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = VoltCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = VoltTextSecondary)
    }
}
