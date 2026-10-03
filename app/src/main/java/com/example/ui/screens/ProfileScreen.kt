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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userProfile.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VoltGreen)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BT",
                        color = VoltDarkBg,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = VoltTextSecondary
                    )
                    Text(
                        text = "Indore, Madhya Pradesh",
                        fontSize = 11.sp,
                        color = VoltCyan
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(36.dp)
                        .background(VoltCard, CircleShape)
                        .border(1.dp, VoltCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = VoltTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 2. EV Vehicle Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp)),
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
                        Text(text = "EV Vehicle", fontSize = 12.sp, color = VoltTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Tata Nexon EV", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VoltTextPrimary)
                        Text(text = "Range: 180 km • 40.5 kWh", fontSize = 12.sp, color = VoltGreen)
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp, 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.futuristic_ev_car),
                            contentDescription = "EV Car",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // 3. Profile Navigation Menu
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ProfileMenuItem(
                        icon = Icons.Outlined.EvStation,
                        title = "Charging History",
                        onClick = { viewModel.navigateTo(Screen.CHARGING_HISTORY) }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.LocationOn,
                        title = "Saved Locations",
                        onClick = { viewModel.navigateTo(Screen.SAVED_LOCATIONS) }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        badge = if (unreadCount > 0) unreadCount.toString() else null,
                        onClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Calculate,
                        title = "Trip Estimator",
                        onClick = { viewModel.navigateTo(Screen.TRIP_ESTIMATOR) }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Analytics,
                        title = "Admin & Analytics Dashboard",
                        onClick = { viewModel.navigateTo(Screen.ADMIN_DASHBOARD) }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Help & Support",
                        onClick = { }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Settings,
                        title = "Settings",
                        onClick = { }
                    )
                }
            }
        }

        // 4. Logout Button
        item {
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VoltCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = VoltRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", color = VoltRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = VoltCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = VoltTextPrimary)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = VoltGreen,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoltDarkBg)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = VoltTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = VoltCardBorder.copy(alpha = 0.5f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
