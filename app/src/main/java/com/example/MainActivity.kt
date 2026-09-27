package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.kiosk.KioskScreen
import com.example.ui.theme.KioskDarkBackground
import com.example.ui.theme.SmartPresensiTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.AttendanceViewModelFactory

enum class AppScreen {
    KIOSK_MAIN,
    ADMIN_PORTAL
}

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels {
        AttendanceViewModelFactory(application as AttendanceKioskApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartPresensiTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.KIOSK_MAIN) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KioskDarkBackground)
                        .safeDrawingPadding()
                ) {
                    when (currentScreen) {
                        AppScreen.KIOSK_MAIN -> {
                            KioskScreen(
                                viewModel = viewModel,
                                onOpenAdmin = { currentScreen = AppScreen.ADMIN_PORTAL }
                            )
                        }
                        AppScreen.ADMIN_PORTAL -> {
                            BackHandler {
                                currentScreen = AppScreen.KIOSK_MAIN
                            }
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onBackToKiosk = { currentScreen = AppScreen.KIOSK_MAIN }
                            )
                        }
                    }
                }
            }
        }
    }
}
