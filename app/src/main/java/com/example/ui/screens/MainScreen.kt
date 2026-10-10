package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.MainTab
import com.example.ui.Screen
import com.example.ui.VoltEliteViewModel
import com.example.ui.components.InteractiveChargerMap
import com.example.ui.components.VoltBottomBar
import com.example.ui.theme.VoltDarkBg

@Composable
fun MainScreen(
    viewModel: VoltEliteViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val allStations by viewModel.filteredStations.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val carsOnlyFilter by viewModel.carsOnlyFilter.collectAsState()

    BackHandler(enabled = currentTab != MainTab.HOME) {
        viewModel.setTab(MainTab.HOME)
    }

    Scaffold(
        bottomBar = {
            VoltBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VoltDarkBg)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(viewModel = viewModel)
                }
                MainTab.MAP -> {
                    InteractiveChargerMap(
                        stations = allStations,
                        selectedStation = selectedStation,
                        userLocation = userLocation,
                        carsOnlyFilter = carsOnlyFilter,
                        onCarsOnlyToggle = { viewModel.setCarsOnlyFilter(it) },
                        onRefreshLocation = { lat, lon -> viewModel.updateUserLocation(lat, lon) },
                        onStationSelect = { viewModel.selectStation(it) },
                        onNavigateClick = {
                            viewModel.selectStation(it)
                            viewModel.navigateTo(Screen.LIVE_ROUTE)
                        },
                        onDetailsClick = { viewModel.selectStation(it) },
                        onScanQrClick = { viewModel.startQrScan() }
                    )
                }
                MainTab.TRIPS -> {
                    TripsScreen(viewModel = viewModel)
                }
                MainTab.PROFILE -> {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
