package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecommendedChargingStop
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun RoutePlannerScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val origin by viewModel.routeOrigin.collectAsState()
    val dest by viewModel.routeDestination.collectAsState()
    val evModel by viewModel.selectedVehicleModel.collectAsState()
    val currentRange by viewModel.currentRangeKm.collectAsState()
    val routePlan by viewModel.routePlan.collectAsState()

    LaunchedEffect(Unit) {
        if (routePlan == null) {
            viewModel.calculateRoute()
        }
    }

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
                    text = "Plan Your Route",
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
                    onClick = { viewModel.navigateTo(Screen.LIVE_ROUTE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                        .testTag("start_live_route_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Start", tint = VoltDarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Live Navigation",
                        color = VoltDarkBg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Origin & Destination Inputs
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "From", fontSize = 12.sp, color = VoltTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(VoltGreen, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = origin,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = VoltCardBorder,
                            thickness = 0.8.dp
                        )

                        Text(text = "To", fontSize = 12.sp, color = VoltTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(VoltCyan, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dest,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                        }
                    }
                }
            }

            // 2. EV Model Selector Card
            item {
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
                            Icon(Icons.Default.DirectionsCar, contentDescription = "Car", tint = VoltCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "EV Model", fontSize = 11.sp, color = VoltTextSecondary)
                                Text(text = evModel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select", tint = VoltTextSecondary)
                    }
                }
            }

            // 3. Current Range Slider
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Current Range", fontSize = 13.sp, color = VoltTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentRange km",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = VoltGreen, modifier = Modifier.size(18.dp))
                            }
                        }

                        Slider(
                            value = currentRange.toFloat(),
                            onValueChange = { viewModel.setCurrentRangeKm(it.toInt()) },
                            valueRange = 40f..400f,
                            colors = SliderDefaults.colors(
                                thumbColor = VoltGreen,
                                activeTrackColor = VoltGreen,
                                inactiveTrackColor = VoltCardElevated
                            )
                        )
                    }
                }
            }

            // 4. Distance and Estimated Time summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = VoltCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Distance", fontSize = 11.sp, color = VoltTextSecondary)
                            Text(
                                "${routePlan?.distanceKm ?: 196} km",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VoltTextPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = VoltCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Estimated Time", fontSize = 11.sp, color = VoltTextSecondary)
                            Text(
                                routePlan?.durationFormatted ?: "3 hr 12 min",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VoltTextPrimary
                            )
                        }
                    }
                }
            }

            // 5. Button to re-calculate stops
            item {
                Button(
                    onClick = { viewModel.calculateRoute() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Optimize", tint = VoltGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Find Charging Stops", color = VoltGreen, fontWeight = FontWeight.Bold)
                }
            }

            // 6. Recommended Stops Section
            item {
                Text(
                    text = "Recommended Stops (${routePlan?.stops?.size ?: 0})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }

            items(routePlan?.stops ?: emptyList()) { stop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(14.dp))
                        .clickable { viewModel.selectStation(stop.station) },
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(VoltGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EvStation, contentDescription = "Station", tint = VoltGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stop.station.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltTextPrimary
                                )
                                Text(
                                    text = "${stop.distanceFromStartKm} km • ${stop.drivingTimeMin} min drive • ${stop.suggestedChargeMin}m charge",
                                    fontSize = 12.sp,
                                    color = VoltTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated
                        ) {
                            Text(
                                text = "Fast",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun LiveRouteScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val routePlan by viewModel.routePlan.collectAsState()
    val plan = routePlan ?: viewModel.repository.calculateRoute("Indore", "Bhopal", 180, "Tata Nexon EV")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        // Top Route Header
        Surface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            color = VoltCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.ROUTE_PLANNER) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Indore → Bhopal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = "196 km • 3 hr 12 min • Battery: 68%",
                        fontSize = 12.sp,
                        color = VoltGreen
                    )
                }
            }
        }

        // Live Route Vector Map Visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF090D17))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Waypoint points
                val p1 = Offset(w * 0.15f, h * 0.8f) // Indore
                val p2 = Offset(w * 0.35f, h * 0.55f) // Dewas
                val p3 = Offset(w * 0.60f, h * 0.40f) // Ujjain
                val p4 = Offset(w * 0.85f, h * 0.20f) // Bhopal

                // Highway path
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(p1.x, p1.y)
                    quadraticTo(p2.x - 20f, p2.y + 20f, p2.x, p2.y)
                    quadraticTo(p3.x - 10f, p3.y + 10f, p3.x, p3.y)
                    quadraticTo(p4.x - 20f, p4.y + 10f, p4.x, p4.y)
                }

                drawPath(
                    path = path,
                    color = VoltGreen,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                )

                // Route glow
                drawPath(
                    path = path,
                    color = VoltGreenGlow,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24f)
                )

                // Dots
                drawCircle(color = VoltCyan, radius = 12f, center = p1)
                drawCircle(color = VoltGreen, radius = 12f, center = p2)
                drawCircle(color = VoltGreen, radius = 12f, center = p3)
                drawCircle(color = VoltPurple, radius = 14f, center = p4)
            }

            // Next charger heads-up banner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = VoltCard.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "Next", tint = VoltGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Next Charger: Tata Power — Dewas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VoltTextPrimary)
                            Text("In 52 km • 45 min", fontSize = 11.sp, color = VoltCyan)
                        }
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.CHARGING_SESSION) },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Charge Now", color = VoltDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Waypoints timeline list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Live Route Steps", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
            }

            item {
                WaypointItem(
                    label = "Indore (Start)",
                    sub = "Battery: 68% • 180 km range",
                    dotColor = VoltCyan,
                    isDone = true
                )
            }

            item {
                WaypointItem(
                    label = "Tata Power — Dewas",
                    sub = "52 km • 45 min (Charging 20 min) • Fast 60 kW",
                    dotColor = VoltGreen,
                    isDone = false
                )
            }

            item {
                WaypointItem(
                    label = "ChargeZone — Ujjain Bypass",
                    sub = "98 km • 1 hr 40 min (Charging 25 min) • Fast 120 kW",
                    dotColor = VoltGreen,
                    isDone = false
                )
            }

            item {
                WaypointItem(
                    label = "Bhopal (Destination)",
                    sub = "155 km • 3 hr 12 min • Est. Battery 55%",
                    dotColor = VoltPurple,
                    isDone = false
                )
            }
        }
    }
}

@Composable
private fun WaypointItem(
    label: String,
    sub: String,
    dotColor: Color,
    isDone: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, VoltCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = VoltCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(dotColor, CircleShape)
                    .border(2.dp, VoltDarkBg, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VoltTextPrimary)
                Text(text = sub, fontSize = 11.sp, color = VoltTextSecondary)
            }
        }
    }
}
