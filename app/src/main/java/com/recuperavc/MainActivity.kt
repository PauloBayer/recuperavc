package com.recuperavc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.recuperavc.ui.home.HomeScreen
import com.recuperavc.ui.main.AudioAnalysisScreen
import com.recuperavc.ui.main.MainScreenViewModel
import com.recuperavc.ui.theme.WhisperCppDemoTheme

enum class AppRoute { Home, AudioAnalysis }

class MainActivity : ComponentActivity() {
    private val viewModel: MainScreenViewModel by viewModels { MainScreenViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhisperCppDemoTheme {
                var route by remember { mutableStateOf(AppRoute.Home) }
                when (route) {
                    AppRoute.Home -> HomeScreen(
                        onOpenAudioTest = { route = AppRoute.AudioAnalysis },
                        onExit = { finishAffinity() }
                    )
                    AppRoute.AudioAnalysis -> AudioAnalysisScreen(
                        viewModel = viewModel,
                        onBack = { route = AppRoute.Home }
                    )
                }
            }
        }
    }
}
