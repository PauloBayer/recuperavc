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
import com.recuperavc.ui.main.SentenceArrangeScreen
import com.recuperavc.ui.main.MotionTestScreen
import com.recuperavc.ui.reports.ReportsScreen
import com.recuperavc.ui.settings.SettingsScreen
import com.recuperavc.ui.theme.WhisperCppDemoTheme

enum class AppRoute { Home, AudioAnalysis, SentenceArrange, MotionTest, Reports, Settings }

class MainActivity : ComponentActivity() {
    private val viewModel: MainScreenViewModel by viewModels { MainScreenViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhisperCppDemoTheme {
                var route by remember { mutableStateOf(AppRoute.Home) }
                when (route) {
                    AppRoute.Home -> HomeScreen(
                        onOpenSentenceTest = { route = AppRoute.SentenceArrange },
                        onOpenAudioTest = { route = AppRoute.AudioAnalysis },
                        onOpenMotionTest = { route = AppRoute.MotionTest },
                        onOpenReports = { route = AppRoute.Reports },
                        onOpenSettings = { route = AppRoute.Settings },
                        onExit = { finishAffinity() }
                    )
                    AppRoute.AudioAnalysis -> AudioAnalysisScreen(
                        viewModel = viewModel,
                        onBack = { route = AppRoute.Home }
                    )
                    AppRoute.SentenceArrange -> SentenceArrangeScreen(
                        phrase = "O rato roeu a roupa do rei de Roma",
                        onResult = { },
                        onBack = { route = AppRoute.Home }
                    )
                    AppRoute.MotionTest -> MotionTestScreen(
                        onFinish = { },
                        onBack = { route = AppRoute.Home }
                    )
                    AppRoute.Reports -> ReportsScreen(
                        onBack = { route = AppRoute.Home }
                    )
                    AppRoute.Settings -> SettingsScreen(
                        onBack = { route = AppRoute.Home }
                    )
                }
            }
        }
    }
}
