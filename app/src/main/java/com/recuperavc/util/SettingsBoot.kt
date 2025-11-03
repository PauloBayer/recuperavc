package com.recuperavc.ui.util

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.recuperavc.models.SettingsViewModel
import kotlinx.coroutines.flow.first

data class InitialSettings(val dark: Boolean, val contrast: Boolean, val scale: Float)

/**
 * Suspends composition until the first persisted values are available.
 * Returns null until all values are loaded, so you can draw a safe placeholder (black).
 */
@Composable
fun rememberInitialSettings(vm: SettingsViewModel): InitialSettings? {
    return produceState<InitialSettings?>(initialValue = null, vm) {
        val d = vm.darkModeFlow.first()
        val c = vm.contrastFlow.first()
        val s = vm.sizeTextFlow.first()
        value = InitialSettings(d, c, s)
    }.value
}

/** Paint status/nav bars immediately (used for the placeholder and first real frame). */
@Composable
fun PaintSystemBars(background: Color, lightIcons: Boolean) {
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.window?.let { w ->
            val argb = background.toArgb()
            w.setBackgroundDrawable(ColorDrawable(argb))
            w.statusBarColor = argb
            w.navigationBarColor = argb
            val ctrl = WindowCompat.getInsetsController(w, w.decorView)
            ctrl.isAppearanceLightStatusBars = lightIcons
            ctrl.isAppearanceLightNavigationBars = lightIcons
        }
    }
}
