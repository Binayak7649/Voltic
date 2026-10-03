package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChargingNetwork
import com.example.ui.VoltEliteViewModel
import com.example.ui.theme.*

@Composable
fun SplashScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        // High-res EV Charging Hub Hero Background
        Image(
            painter = painterResource(id = R.drawable.ev_login_charging_station),
            contentDescription = "EV Charging Super App",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Ultra-luxurious gradient scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to VoltDarkBg.copy(alpha = 0.55f),
                        0.4f to VoltDarkBg.copy(alpha = 0.75f),
                        0.85f to VoltDarkBg.copy(alpha = 0.98f),
                        1.0f to VoltDarkBg
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding with animated electric halo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(VoltGreen.copy(alpha = 0.15f * pulseGlow + 0.1f))
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(
                                listOf(VoltGreen, VoltCyan, VoltGreen)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "VoltElite Logo",
                        tint = VoltGreen,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "VoltElite",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VoltTextPrimary,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "Charge • Explore • Go Further",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltCyan,
                    letterSpacing = 0.8.sp
                )
            }

            // Middle Tagline & Roaming Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VoltCard.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(VoltGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OCPI 2.2 ROAMING ENABLED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "All EV Chargers\nOne Unified Super App",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Network badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Tata Power", "ChargeZone", "Statiq", "Jio-bp", "+ More").forEach { net ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoltCard.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                        ) {
                            Text(
                                text = net,
                                fontSize = 11.sp,
                                color = VoltTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom loading indicator
            CircularProgressIndicator(
                color = VoltGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        // High-Quality EV Supercharging Station Background
        Image(
            painter = painterResource(id = R.drawable.ev_login_charging_station),
            contentDescription = "EV Charging Station",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Rich Atmospheric Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.25f to VoltDarkBg.copy(alpha = 0.45f),
                        0.55f to VoltDarkBg.copy(alpha = 0.88f),
                        0.80f to VoltDarkBg
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glassmorphic Login Container Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(VoltGreen.copy(alpha = 0.5f), VoltCyan.copy(alpha = 0.2f), VoltCardBorder)
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = VoltCard.copy(alpha = 0.88f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Bolt Badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VoltGreen.copy(alpha = 0.2f))
                            .border(1.5.dp, VoltGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Logo",
                            tint = VoltGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "VoltElite",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VoltTextPrimary,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Connect. Charge. Go Further.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltCyan,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Discover, compare & charge across 8+ EV networks with zero roaming friction.",
                        fontSize = 13.sp,
                        color = VoltTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Google Sign In Button (Glassmorphic)
                    Button(
                        onClick = { viewModel.loginWithGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltCardElevated),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            tint = VoltCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Login Button (Luminous Green)
                    Button(
                        onClick = { viewModel.navigateTo(com.example.ui.Screen.PHONE_LOGIN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("phone_login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = "Phone",
                            tint = VoltDarkBg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Phone (+91)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltDarkBg
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guest / Demo mode link
                    TextButton(
                        onClick = { viewModel.exploreAsGuest() },
                        modifier = Modifier.testTag("explore_guest_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Explore Demo Mode",
                                color = VoltCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Demo", tint = VoltCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneLoginScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    var phoneInput by remember { mutableStateOf("9876543210") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        // High-Res Charging Station Background
        Image(
            painter = painterResource(id = R.drawable.ev_login_charging_station),
            contentDescription = "EV Charging Station",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to VoltDarkBg.copy(alpha = 0.75f),
                        0.5f to VoltDarkBg.copy(alpha = 0.90f),
                        1.0f to VoltDarkBg
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                IconButton(
                    onClick = { viewModel.navigateTo(com.example.ui.Screen.WELCOME) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(VoltCard.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, VoltCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Enter Phone Number",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We will send a 6-digit verification code to sign into your VoltElite account.",
                    fontSize = 14.sp,
                    color = VoltTextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Phone Input Field with +91 Country Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VoltCard.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoltCardBorder),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🇮🇳 +91", fontWeight = FontWeight.Bold, color = VoltTextPrimary, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 10) phoneInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        placeholder = { Text("Enter 10-digit number", color = VoltTextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phone_input_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VoltCard.copy(alpha = 0.9f),
                            unfocusedContainerColor = VoltCard.copy(alpha = 0.9f),
                            focusedBorderColor = VoltGreen,
                            unfocusedBorderColor = VoltCardBorder,
                            focusedTextColor = VoltTextPrimary,
                            unfocusedTextColor = VoltTextPrimary
                        )
                    )
                }
            }

            Button(
                onClick = { viewModel.startPhoneAuth(phoneInput) },
                enabled = phoneInput.length == 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("send_otp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Send OTP",
                    color = VoltDarkBg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OtpVerifyScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    var otpInput by remember { mutableStateOf("749216") }
    val cooldown by viewModel.otpCooldownSeconds.collectAsState()
    val isVerifying by viewModel.isOtpVerifying.collectAsState()
    val phone by viewModel.phoneNumber.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        // High-Res Charging Station Background
        Image(
            painter = painterResource(id = R.drawable.ev_login_charging_station),
            contentDescription = "EV Charging Station",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to VoltDarkBg.copy(alpha = 0.8f),
                        0.5f to VoltDarkBg.copy(alpha = 0.92f),
                        1.0f to VoltDarkBg
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                IconButton(
                    onClick = { viewModel.navigateTo(com.example.ui.Screen.PHONE_LOGIN) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(VoltCard.copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, VoltCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = VoltTextPrimary)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Verification Code",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the 6-digit OTP sent to +91 $phone",
                    fontSize = 14.sp,
                    color = VoltTextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // OTP Input Field
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { if (it.length <= 6) otpInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VoltCard.copy(alpha = 0.92f),
                        unfocusedContainerColor = VoltCard.copy(alpha = 0.92f),
                        focusedBorderColor = VoltGreen,
                        unfocusedBorderColor = VoltCardBorder,
                        focusedTextColor = VoltGreen,
                        unfocusedTextColor = VoltTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cooldown > 0) {
                        Text(
                            text = "Resend OTP in ${cooldown}s",
                            fontSize = 13.sp,
                            color = VoltTextSecondary
                        )
                    } else {
                        TextButton(onClick = { viewModel.resendOtp() }) {
                            Text(text = "Resend OTP", color = VoltCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = { viewModel.navigateTo(com.example.ui.Screen.PHONE_LOGIN) }) {
                        Text(text = "Change Number", color = VoltTextSecondary)
                    }
                }
            }

            Button(
                onClick = { viewModel.verifyOtp(otpInput) },
                enabled = otpInput.length == 6 && !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_otp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = VoltDarkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Verify & Continue",
                        color = VoltDarkBg,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
