package com.recuperavc.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.HighContrastAccent

data class ReportsPalette(
    val bg: Color,                 // fundo da tela
    val sheet: Color,              // cartões grandes/folha
    val surface: Color,            // cartões padrão
    val surfaceVariant: Color,     // caixas e “chips” de fundo
    val borderSoft: Color?,        // borda sutil para dark/HC
    val textPrimary: Color,
    val textSecondary: Color,
    val heading: Color,            // títulos
    val accent: Color,             // cor de ação/seleção
    val chartBar: Color,           // barras dos gráficos
    val gridLine: Color,           // linhas da grade
    val chipBg: Color,
    val chipText: Color
)

private val LightPalette = ReportsPalette(
    bg = Color.White,
    sheet = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    borderSoft = null,
    textPrimary = Color.Black,
    textSecondary = Color.Black.copy(alpha = 0.7f),
    heading = GreenDark,
    accent = GreenDark,
    chartBar = GreenDark,
    gridLine = Color(0xFFE0E0E0),
    chipBg = GreenLight.copy(alpha = 0.25f),
    chipText = GreenDark
)

private val DarkPalette = ReportsPalette(
    bg = Color(0xFF101010),
    sheet = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF232323),
    borderSoft = Color.White.copy(alpha = 0.06f),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFFEDEDED).copy(alpha = 0.72f),
    heading = Color.White,
    accent = GreenLight, // leve contraste no dark
    chartBar = GreenLight,
    gridLine = Color.White.copy(alpha = 0.08f),
    chipBg = Color.White.copy(alpha = 0.08f),
    chipText = Color.White
)

private val HighContrastPalette = ReportsPalette(
    bg = Color.Black,
    sheet = Color.Black,
    surface = Color(0xFF0B0B0B),
    surfaceVariant = Color(0xFF181818),
    borderSoft = Color.White.copy(alpha = 0.18f),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.86f),
    heading = HighContrastAccent,
    accent = HighContrastAccent,
    chartBar = HighContrastAccent,
    gridLine = Color.White.copy(alpha = 0.14f),
    chipBg = HighContrastAccent,
    chipText = Color.Black
)

val LocalReportsPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ProvideReportsPalette(
    appliedDark: Boolean,
    appliedContrast: Boolean,
    content: @Composable () -> Unit
) {
    val palette = remember(appliedDark, appliedContrast) {
        when {
            appliedContrast -> HighContrastPalette
            appliedDark -> DarkPalette
            else -> LightPalette
        }
    }
    CompositionLocalProvider(LocalReportsPalette provides palette, content = content)
}