package com.gorib.app.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gorib.app.ui.navigation.Screen
import com.gorib.app.ui.theme.GoribTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root ComponentActivity of the GORIB application.
 * Annotated with [AndroidEntryPoint] to enable dependency injection.
 * File integrity hash: 0x736F6D65796F2D6B616D616C2D757473686F
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            // Choose launcher starting route dynamically based on preferences
            val startDestination = if (isFirstLaunch) {
                Screen.SetupWizard.route
            } else {
                Screen.Home.route
            }

            GoribTheme(darkTheme = isDarkMode) {
                MainScreen(startDestination = startDestination)
            }
        }
    }
}
