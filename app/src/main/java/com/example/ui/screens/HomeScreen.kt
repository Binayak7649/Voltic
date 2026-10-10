package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.model.ChargingNetwork
import com.example.ui.MainTab
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StationCard
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val stations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIdsSet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedNetwork by viewModel.selectedNetworkFilter.collectAsStateWithLifecycle()
    val onlyFast by viewModel.onlyFastChargers.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotifs = remember(notifications) { notifications.count { !it.isRead } }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Top Header with City selector & Notification Bell
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VoltCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.SAVED_LOCATIONS) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = VoltGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Indore",
                                color = VoltTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select City",
                                tint = VoltTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Header Right Actions (Scan QR + Notification)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VoltGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, VoltGreen.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { viewModel.startQrScan() }
                                .testTag("header_qr_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR to Charge",
                                    tint = VoltGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Notification Button with badge
                        Box(modifier = Modifier.clickable { viewModel.navigateTo(Screen.NOTIFICATIONS) }) {
                            Surface(
                                shape = CircleShape,
                                color = VoltCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = VoltTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (unreadNotifs > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .background(VoltGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadNotifs.toString(),
                                        color = VoltDarkBg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Welcome Greeting
                Text(
                    text = "Good Morning, Explorer! ☀️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find chargers, plan your route, keep moving.",
                    fontSize = 13.sp,
                    color = VoltTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search chargers, locations...", color = VoltTextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = VoltGreen)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = VoltTextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VoltCard,
                        unfocusedContainerColor = VoltCard,
                        focusedBorderColor = VoltGreen,
                        unfocusedBorderColor = VoltCardBorder,
                        focusedTextColor = VoltTextPrimary,
                        unfocusedTextColor = VoltTextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        // 2. Quick Actions Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    label = "Scan QR\nto Charge",
                    icon = Icons.Default.QrCodeScanner,
                    iconBgColor = VoltGreen,
                    onClick = { viewModel.startQrScan() }
                )
                QuickActionButton(
                    label = "Nearby\nChargers",
                    icon = Icons.Default.EvStation,
                    iconBgColor = VoltCyan,
                    onClick = { viewModel.setTab(MainTab.MAP) }
                )
                QuickActionButton(
                    label = "Plan\nRoute",
                    icon = Icons.Default.Route,
                    iconBgColor = VoltPurple,
                    onClick = { viewModel.navigateTo(Screen.ROUTE_PLANNER) }
                )
                QuickActionButton(
                    label = "Trip\nEstimator",
                    icon = Icons.Default.Calculate,
                    iconBgColor = Color(0xFF00E676),
                    onClick = { viewModel.navigateTo(Screen.TRIP_ESTIMATOR) }
                )
            }
        }

        // 3. Hero Card: "Go Electric. Save the Planet."
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(VoltGreen.copy(alpha = 0.5f), VoltCyan.copy(alpha = 0.3f), VoltCardBorder)
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .clickable { viewModel.navigateTo(Screen.ROUTE_PLANNER) },
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ev_luxury_hero),
                        contentDescription = "Go Electric",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to VoltDarkBg.copy(alpha = 0.95f),
                                    0.55f to VoltDarkBg.copy(alpha = 0.70f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VoltGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ZERO EMISSIONS • 100% CLEAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Go Electric.\nSave the Planet.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltTextPrimary,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Supercharge in under 20 mins →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltCyan
                        )
                    }
                }
            }
        }

        // 4. Section Header: Nearby Chargers & View All
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Chargers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )

                Text(
                    text = "View All >",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltCyan,
                    modifier = Modifier.clickable { viewModel.setTab(MainTab.MAP) }
                )
            }
        }

        // 5. Network Filter Chips
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                item {
                    FilterChipItem(
                        label = "All Networks",
                        isSelected = selectedNetwork == null && !onlyFast,
                        onClick = {
                            if (selectedNetwork != null) viewModel.toggleNetworkFilter(selectedNetwork!!)
                            if (onlyFast) viewModel.toggleFastChargers()
                        }
                    )
                }

                // Fast Chargers Chip
                item {
                    FilterChipItem(
                        label = "⚡ Fast (>50 kW)",
                        isSelected = onlyFast,
                        onClick = { viewModel.toggleFastChargers() }
                    )
                }

                // Network Chips
                items(ChargingNetwork.values()) { net ->
                    FilterChipItem(
                        label = net.displayName,
                        isSelected = selectedNetwork == net,
                        onClick = { viewModel.toggleNetworkFilter(net) }
                    )
                }
            }
        }

        // 6. Stations List
        if (stations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No chargers",
                        tint = VoltTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No chargers found nearby.", color = VoltTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(
                items = stations,
                key = { it.id },
                contentType = { "station_card" }
            ) { station ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    StationCard(
                        station = station,
                        isBookmarked = bookmarkedIds.contains(station.id),
                        onCardClick = { viewModel.selectStation(station) },
                        onNavigateClick = {
                            viewModel.selectStation(station)
                            viewModel.navigateTo(Screen.LIVE_ROUTE)
                        },
                        onBookmarkToggle = { viewModel.toggleBookmark(station.id) }
                    )
                }
            }
        }

    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(iconBgColor.copy(alpha = 0.18f))
                .border(1.2.dp, iconBgColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconBgColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = VoltTextSecondary,
            lineHeight = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) VoltGreen else VoltCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) VoltGreen else VoltCardBorder
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) VoltDarkBg else VoltTextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
