package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltDarkBg

class MainActivity : ComponentActivity() {
    private val viewModel: VoltEliteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoltEliteApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VoltEliteApp(viewModel: VoltEliteViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Global back handling for secondary sub-screens
    if (currentScreen != Screen.SPLASH && currentScreen != Screen.WELCOME && currentScreen != Screen.MAIN) {
        BackHandler {
            viewModel.navigateTo(Screen.MAIN)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoltDarkBg)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.SPLASH -> SplashScreen(viewModel = viewModel)
                Screen.WELCOME -> WelcomeScreen(viewModel = viewModel)
                Screen.PHONE_LOGIN -> PhoneLoginScreen(viewModel = viewModel)
                Screen.OTP_VERIFY -> OtpVerifyScreen(viewModel = viewModel)
                Screen.MAIN -> MainScreen(viewModel = viewModel)
                Screen.CHARGER_DETAILS -> ChargerDetailsScreen(viewModel = viewModel)
                Screen.ROUTE_PLANNER -> RoutePlannerScreen(viewModel = viewModel)
                Screen.LIVE_ROUTE -> LiveRouteScreen(viewModel = viewModel)
                Screen.CHARGING_SESSION -> ChargingSessionScreen(viewModel = viewModel)
                Screen.TRIP_ESTIMATOR -> TripEstimatorScreen(viewModel = viewModel)
                Screen.ADMIN_DASHBOARD -> AdminAnalyticsScreen(viewModel = viewModel)
                Screen.SAVED_LOCATIONS -> SavedLocationsScreen(viewModel = viewModel)
                Screen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                Screen.QR_SCANNER -> QRScannerScreen(viewModel = viewModel)
                Screen.CHARGER_VERIFICATION -> ChargerVerificationScreen(viewModel = viewModel)
                Screen.CHARGING_RECEIPT -> ChargingReceiptScreen(viewModel = viewModel)
                Screen.CHARGING_HISTORY -> ChargingHistoryScreen(viewModel = viewModel)
                else -> MainScreen(viewModel = viewModel)
            }
        }
    }
}
