package com.recuperavc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings

private val HighContrastAccent = Color(0xFFFFD600)

/** Public wrapper that waits for real settings to avoid 1-frame light flash. */
@Composable
fun RecuperAVCBrandHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    // Block until we have the persisted values (same pattern you use in other screens)
    val initial: InitialSettings? = rememberInitialSettings(settings)
    if (initial == null) {
        // draw nothing so the parent background shows; no white flash
        Spacer(Modifier.height(0.dp))
        return
    }

    val appliedDark by settings.darkModeFlow.collectAsState(initial = initial.dark)
    val appliedContrast by settings.contrastFlow.collectAsState(initial = initial.contrast)
    val appliedScale by settings.sizeTextFlow.collectAsState(initial = initial.scale)

    RecuperAVCBrandHeaderThemed(
        modifier = modifier,
        appliedContrast = appliedContrast,
        appliedDark = appliedDark,
        appliedScale = appliedScale
    )
}

@Composable
private fun RecuperAVCBrandHeaderThemed(
    modifier: Modifier = Modifier,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val containerColor = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val contentText = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEAEAEA)
        else -> Color.Black
    }
    val accent = if (appliedContrast) HighContrastAccent else GreenDark

    // Remember brushes so they don't get recreated during recompositions
    val textGradient = remember(appliedContrast) {
        if (appliedContrast) null else Brush.horizontalGradient(listOf(GreenDark, GreenLight))
    }
    val ribbonBrush = remember(appliedContrast) {
        if (appliedContrast) null else Brush.horizontalGradient(listOf(GreenLight, GreenDark))
    }

    val cardShape = RoundedCornerShape(20.dp)
    val ribbonShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (appliedContrast) Modifier.border(2.dp, accent, cardShape) else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast) 0.dp else 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Top ribbon (tagline) — choose color OR brush (never both)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .let { base ->
                        if (appliedContrast) {
                            base.background(color = HighContrastAccent, shape = ribbonShape)
                        } else {
                            base.background(brush = ribbonBrush!!, shape = ribbonShape)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Recuperação guiada por dados",
                    color = if (appliedContrast) Color.Black else Color.White,
                    fontSize = 12.sp * appliedScale,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (appliedContrast) {
                    Text(
                        text = "RecuperAVC",
                        color = contentText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp * appliedScale,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp * appliedScale
                    )
                } else {
                    val brand = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                brush = textGradient!!,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp * appliedScale,
                                letterSpacing = 0.5.sp
                            )
                        ) {
                            append("Recuper")
                            append("AVC")
                        }
                    }
                    Text(text = brand, textAlign = TextAlign.Center, lineHeight = 34.sp * appliedScale)
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Frases • Voz • Coordenação",
                    color = if (appliedContrast) contentText else GreenDark.copy(alpha = 0.75f),
                    fontSize = 13.sp * appliedScale,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
