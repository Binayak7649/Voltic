package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChargingStation
import com.example.model.StationStatus
import com.example.ui.MainTab
import com.example.ui.theme.*

@Composable
fun VoltBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = VoltDarkBg,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = VoltCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoltTabItem(
                    label = "Home",
                    icon = Icons.Filled.Home,
                    isSelected = currentTab == MainTab.HOME,
                    onClick = { onTabSelected(MainTab.HOME) }
                )
                VoltTabItem(
                    label = "Map",
                    icon = Icons.Filled.Map,
                    isSelected = currentTab == MainTab.MAP,
                    onClick = { onTabSelected(MainTab.MAP) }
                )
                VoltTabItem(
                    label = "Trips",
                    icon = Icons.Filled.DirectionsCar,
                    isSelected = currentTab == MainTab.TRIPS,
                    onClick = { onTabSelected(MainTab.TRIPS) }
                )
                VoltTabItem(
                    label = "Profile",
                    icon = Icons.Filled.Person,
                    isSelected = currentTab == MainTab.PROFILE,
                    onClick = { onTabSelected(MainTab.PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun VoltTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("tab_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (isSelected) {
                Modifier
                    .size(36.dp)
                    .background(VoltGreen.copy(alpha = 0.15f), CircleShape)
            } else {
                Modifier.size(36.dp)
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) VoltGreen else VoltTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) VoltGreen else VoltTextSecondary
        )
    }
}

@Composable
fun StationCard(
    station: ChargingStation,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("station_card_${station.id}"),
        colors = CardDefaults.cardColors(containerColor = VoltCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(station.network.brandColorHex).copy(alpha = 0.2f))
                            .border(1.dp, Color(station.network.brandColorHex).copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
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
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VoltTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${station.distanceKm} km • ${station.etaMinutes} min • ${station.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VoltTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Station",
                        tint = if (isBookmarked) VoltGreen else VoltTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Availability, Speed, Connectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Availability Pill
                val availColor = if (station.status == StationStatus.AVAILABLE) VoltGreen else VoltYellow
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = availColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, availColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(availColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Available ${station.totalAvailable}/${station.totalPorts}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = availColor
                        )
                    }
                }

                // Speed Pill
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

                // Connectors Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VoltCardElevated
                ) {
                    Text(
                        text = station.connectors.joinToString(", ") { it.type.displayName },
                        fontSize = 11.sp,
                        color = VoltTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = VoltCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Price and Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${station.pricePerKwh.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = " / kWh",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoltTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "★ ${station.rating}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltYellow
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onNavigateClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCyan.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Navigate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onCardClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "View Station",
                            color = VoltDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularChargingGauge(
    currentPercent: Float,
    targetPercent: Float = 85f,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = this.size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Outer glow ring
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(VoltGreen.copy(alpha = glowAlpha * 0.4f), Color.Transparent),
                    center = center,
                    radius = diameter / 1.5f
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true
            )

            // Background Track
            drawArc(
                color = VoltCardElevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            val progressSweep = (currentPercent / 100f) * 270f
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to VoltCyan,
                    0.6f to VoltGreen,
                    1.0f to VoltGreen,
                    center = center
                ),
                startAngle = 135f,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(VoltGreen.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, VoltGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = VoltGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${currentPercent.toInt()}%",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VoltTextPrimary
            )
            Text(
                text = "Fast Charging",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VoltGreen
            )
        }
    }
}
