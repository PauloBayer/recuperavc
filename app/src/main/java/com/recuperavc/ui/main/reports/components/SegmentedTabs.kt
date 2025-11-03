package com.recuperavc.ui.main.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.ui.main.reports.type.ReportTab
import com.recuperavc.ui.theme.LocalReportsPalette
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings

private fun Color.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
private val Color.on: Color get() = if (luma() > 0.6f) Color.Black else Color.White

@Composable
fun SegmentedTabs(tab: ReportTab, onTab: (ReportTab) -> Unit) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val scale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(p.surfaceVariant)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Audio,
            icon = Icons.Default.Mic,
            text = "Voz",
            selectedBg = p.accent,
            selectedFg = p.accent.on,
            unselectedBg = p.surface,
            unselectedFg = p.textPrimary,
            border = p.borderSoft?.let { BorderStroke(1.dp, it) },
            scale = scale
        ) { onTab(ReportTab.Audio) }

        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Coherence,
            icon = Icons.Default.TrendingUp,
            text = "Raciocínio",
            selectedBg = p.accent,
            selectedFg = p.accent.on,
            unselectedBg = p.surface,
            unselectedFg = p.textPrimary,
            border = p.borderSoft?.let { BorderStroke(1.dp, it) },
            scale = scale
        ) { onTab(ReportTab.Coherence) }

        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Motion,
            icon = Icons.Default.Gesture,
            text = "Coordenação",
            selectedBg = p.accent,
            selectedFg = p.accent.on,
            unselectedBg = p.surface,
            unselectedFg = p.textPrimary,
            border = p.borderSoft?.let { BorderStroke(1.dp, it) },
            scale = scale
        ) { onTab(ReportTab.Motion) }
    }
}

@Composable
private fun SegButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    text: String,
    selectedBg: Color,
    selectedFg: Color,
    unselectedBg: Color,
    unselectedFg: Color,
    border: BorderStroke?,
    scale: Float,
    onClick: () -> Unit
) {
    val bg = if (selected) selectedBg else unselectedBg
    val fg = if (selected) selectedFg else unselectedFg
    val elev = if (selected && border == null) 4.dp else 0.dp

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = elev),
        border = if (selected) null else border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = (13.sp * scale), textAlign = TextAlign.Center)
        }
    }
}
