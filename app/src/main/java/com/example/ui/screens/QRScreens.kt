package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChargingSessionEntity
import com.example.model.*
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun QRScannerScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    var manualInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    val errorMessage by viewModel.qrErrorMessage.collectAsState()
    val presets = remember { viewModel.qrService.getDemoPresets() }

    BackHandler {
        viewModel.navigateTo(Screen.MAIN)
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.MAIN) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(VoltCard, CircleShape)
                        .border(1.dp, VoltCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }

                Text(
                    text = "Scan QR to Charge",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )

                IconButton(
                    onClick = { isTorchOn = !isTorchOn },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (isTorchOn) VoltGreen.copy(alpha = 0.2f) else VoltCard, CircleShape)
                        .border(1.dp, if (isTorchOn) VoltGreen else VoltCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) VoltGreen else VoltTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Align camera with the QR code on the charger or EVSE",
                fontSize = 13.sp,
                color = VoltTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Camera Viewfinder Box with Laser Animation
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(VoltSurface)
                    .border(2.dp, VoltCardBorder, RoundedCornerShape(24.dp))
                    .testTag("qr_scanner_viewfinder"),
                contentAlignment = Alignment.Center
            ) {
                // High-tech viewfinder overlay canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cornerLen = 42f
                    val strokeW = 6f

                    // Draw 4 corner brackets
                    // Top-Left
                    drawLine(VoltGreen, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeW)
                    drawLine(VoltGreen, Offset(16f, 16f), Offset(16f, 16f + cornerLen), strokeW)
                    // Top-Right
                    drawLine(VoltGreen, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), strokeW)
                    drawLine(VoltGreen, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeW)
                    // Bottom-Left
                    drawLine(VoltGreen, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeW)
                    drawLine(VoltGreen, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), strokeW)
                    // Bottom-Right
                    drawLine(VoltGreen, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), strokeW)
                    drawLine(VoltGreen, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), strokeW)

                    // Moving Laser Line
                    val currentY = h * laserPosition
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, VoltGreen, VoltCyan, VoltGreen, Color.Transparent)
                        ),
                        start = Offset(24f, currentY),
                        end = Offset(w - 24f, currentY),
                        strokeWidth = 4f
                    )
                }

                // Center Guide Icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan",
                        tint = VoltCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scanning...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VoltTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Demo Presets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Quick Demo Scans",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltCyan
                    )
                    Text(
                        text = "Tap to simulate scan",
                        fontSize = 11.sp,
                        color = VoltTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { (title, payload) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VoltCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                            modifier = Modifier.clickable { viewModel.processQrCode(payload) }
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Manual Charger ID Entry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, VoltCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = VoltCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Can't scan? Enter Charger ID manually",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = VoltTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            placeholder = { Text("e.g. EVSE-08", color = VoltTextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_charger_id_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = VoltCardElevated,
                                unfocusedContainerColor = VoltCardElevated,
                                focusedBorderColor = VoltGreen,
                                unfocusedBorderColor = VoltCardBorder,
                                focusedTextColor = VoltTextPrimary,
                                unfocusedTextColor = VoltTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { viewModel.processQrCode(manualInput) },
                            enabled = manualInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("submit_manual_charger_id_button")
                        ) {
                            Text("Verify", color = VoltDarkBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Error Dialog / Banner
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissQrError() },
                containerColor = VoltCard,
                icon = {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = VoltRed, modifier = Modifier.size(36.dp))
                },
                title = {
                    Text(
                        text = "Invalid or Unrecognized QR",
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                },
                text = {
                    Text(
                        text = errorMessage ?: "Unable to verify this charger code.",
                        fontSize = 14.sp,
                        color = VoltTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissQrError() },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Scan Again", color = VoltDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun ChargerVerificationScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val verification by viewModel.chargerVerification.collectAsState()
    val selectedPayment by viewModel.selectedPaymentMethod.collectAsState()

    BackHandler {
        viewModel.navigateTo(Screen.QR_SCANNER)
    }

    if (verification == null) {
        Box(
            modifier = modifier.fillMaxSize().background(VoltDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No charger verified", color = VoltTextSecondary)
        }
        return
    }

    val charger = verification!!.charger
    val compatibility = verification!!.compatibility
    val estimatedCost = verification!!.estimatedFullCost
    val estimatedDuration = verification!!.estimatedDurationMin

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.QR_SCANNER) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Charger Found",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = VoltDarkBg,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (compatibility.isCompatible && charger.supportsRemoteStart) {
                        Button(
                            onClick = { viewModel.confirmStartChargingFromQr() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("confirm_start_charging_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Start", tint = VoltDarkBg, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirm & Start Charging",
                                color = VoltDarkBg,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (!compatibility.isCompatible) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.QR_SCANNER) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltRed.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Incompatible — Scan Another Charger",
                                color = VoltTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.MAIN) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltCyan),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Open ${charger.provider.displayName} App",
                                color = VoltDarkBg,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // 1. Verification Hero Card (Matching Prompt layout!)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(VoltGreen.copy(alpha = 0.5f), VoltCyan.copy(alpha = 0.3f), VoltCardBorder)
                            ),
                            RoundedCornerShape(22.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Verified badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = VoltGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = VoltGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHARGER VERIFIED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = charger.stationName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = charger.location,
                            fontSize = 12.sp,
                            color = VoltTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Specs Grid: Power, Connector, Tariff
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("POWER", fontSize = 10.sp, color = VoltTextMuted, fontWeight = FontWeight.Bold)
                                Text("${charger.powerKw} kW", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VoltCyan)
                                Text("DC Fast", fontSize = 11.sp, color = VoltTextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CONNECTOR", fontSize = 10.sp, color = VoltTextMuted, fontWeight = FontWeight.Bold)
                                Text(charger.connectorType.displayName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VoltTextPrimary)
                                Text("Pin 1", fontSize = 11.sp, color = VoltTextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARIFF", fontSize = 10.sp, color = VoltTextMuted, fontWeight = FontWeight.Bold)
                                Text("₹${charger.tariffPerKwh}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VoltGreen)
                                Text("/ kWh", fontSize = 11.sp, color = VoltTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // EVSE ID Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                        ) {
                            Text(
                                text = "Charger ID: ${charger.evseId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VoltTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 2. Vehicle Compatibility Check
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (compatibility.isCompatible) VoltGreen.copy(alpha = 0.4f) else VoltRed.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (compatibility.isCompatible) VoltGreen.copy(alpha = 0.2f) else VoltRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (compatibility.isCompatible) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = "Compatibility",
                                tint = if (compatibility.isCompatible) VoltGreen else VoltRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (compatibility.isCompatible) "Vehicle Compatibility: Verified" else "Compatibility Warning",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (compatibility.isCompatible) VoltGreen else VoltRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = compatibility.message,
                                fontSize = 12.sp,
                                color = VoltTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 3. Select Payment Method
            item {
                Text(
                    text = "Select Payment Method",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }

            items(PaymentMethod.values()) { method ->
                val isSelected = selectedPayment == method
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (isSelected) VoltGreen else VoltCardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectPaymentMethod(method) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) VoltCardElevated else VoltCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectPaymentMethod(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = VoltGreen, unselectedColor = VoltTextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = method.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VoltTextPrimary
                                )
                                Text(
                                    text = method.subtitle,
                                    fontSize = 11.sp,
                                    color = VoltTextSecondary
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VoltGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Session Estimate Summary
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
                        Column {
                            Text("Estimated Session Duration", fontSize = 11.sp, color = VoltTextSecondary)
                            Text("~$estimatedDuration mins (to 85%)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VoltCyan)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Estimated Pre-Auth", fontSize = 11.sp, color = VoltTextSecondary)
                            Text("₹$estimatedCost", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VoltGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChargingReceiptScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val receipt by viewModel.latestReceipt.collectAsState()

    BackHandler {
        viewModel.navigateTo(Screen.MAIN)
    }

    if (receipt == null) {
        Box(
            modifier = modifier.fillMaxSize().background(VoltDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Receipt not found", color = VoltTextSecondary)
        }
        return
    }

    val r = receipt!!

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = VoltDarkBg
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.MAIN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("receipt_done_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Done",
                            color = VoltDarkBg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.CHARGING_HISTORY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltCyan)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Charging History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // Success Header Icon
            item {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(VoltGreen.copy(alpha = 0.2f))
                        .border(2.dp, VoltGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = VoltGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Charging Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
                Text(
                    text = "Transaction settled successfully",
                    fontSize = 13.sp,
                    color = VoltCyan
                )
            }

            // Digital Receipt Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, VoltCardBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = VoltCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Digital Receipt",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltTextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VoltGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PAID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = VoltCardBorder,
                            thickness = 0.8.dp
                        )

                        ReceiptRow(label = "Station", value = r.stationName)
                        ReceiptRow(label = "Charger / EVSE", value = "${r.evseId} (${r.powerKw} kW)")
                        ReceiptRow(label = "Connector Type", value = r.connectorType.displayName)
                        ReceiptRow(label = "Energy Delivered", value = "${r.energyDeliveredKwh} kWh")
                        ReceiptRow(label = "Charging Duration", value = "${r.chargingTimeMinutes} mins")
                        ReceiptRow(label = "Tariff Rate", value = "₹${r.tariffPerKwh} / kWh")
                        ReceiptRow(label = "Payment Method", value = r.paymentMethod)
                        ReceiptRow(label = "Transaction ID", value = r.txnId)
                        ReceiptRow(label = "Invoice No.", value = r.invoiceNumber)
                        ReceiptRow(label = "Date & Time", value = r.timestampFormatted)

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = VoltCardBorder,
                            thickness = 0.8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Paid",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltTextPrimary
                            )
                            Text(
                                text = "₹ ${r.totalAmountRupees.toInt()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VoltGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChargingHistoryScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.pastSessions.collectAsState()

    BackHandler {
        viewModel.navigateTo(Screen.MAIN)
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
                    text = "Charging History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EvStation, contentDescription = "None", tint = VoltTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No charging sessions yet", color = VoltTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, VoltCardBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.showReceiptFromHistory(session) }
                            .testTag("history_item_${session.id}"),
                        colors = CardDefaults.cardColors(containerColor = VoltCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.stationName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltTextPrimary
                                )
                                Text(
                                    text = "₹ ${session.totalCostRupees.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${session.networkName} • ${session.evseId} • ${session.connectorType} (${session.powerKw} kW)",
                                fontSize = 12.sp,
                                color = VoltCyan
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${session.energyAddedKwh} kWh in ${session.durationMin} mins",
                                    fontSize = 12.sp,
                                    color = VoltTextSecondary
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VoltGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "COMPLETED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = VoltTextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VoltTextPrimary)
    }
}
