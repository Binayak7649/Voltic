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
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun TripsScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val trips by viewModel.trips.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Trips",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VoltTextPrimary
            )

            Button(
                onClick = { viewModel.navigateTo(Screen.ROUTE_PLANNER) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plan", tint = VoltDarkBg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Plan Trip", color = VoltDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tabs: History vs Saved Routes
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp)),
            color = VoltCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                TripTabButton(
                    title = "History",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TripTabButton(
                    title = "Saved Routes",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Trip Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(trips, key = { it.id }) { trip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(Screen.LIVE_ROUTE) }
                        .testTag("trip_card_${trip.id}"),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${trip.origin} → ${trip.destination}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltTextPrimary
                            )
                            Text(
                                text = "₹ ${trip.costRupees}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${trip.distanceKm} km • ${trip.durationFormatted}",
                            fontSize = 13.sp,
                            color = VoltTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trip.dateFormatted,
                                fontSize = 12.sp,
                                color = VoltTextMuted
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VoltGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = trip.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
private fun TripTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) VoltGreen else Color.Transparent
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) VoltDarkBg else VoltTextSecondary
            )
        }
    }
}
