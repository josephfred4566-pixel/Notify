package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.OnboardingVideoScreen
import com.example.ui.dashboard.ToolSelectionDashboard
import com.example.ui.theme.NotifyAppTheme

class MainActivity : ComponentActivity() {

  private val dashboardViewModel: DashboardViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      NotifyAppTheme {
        val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize()) {
          if (uiState.isFirstLaunch) {
            OnboardingVideoScreen(
              onVideoFinished = {
                dashboardViewModel.completeOnboarding()
              }
            )
          } else {
            ToolSelectionDashboard(viewModel = dashboardViewModel)
          }
        }
      }
    }
  }
}
