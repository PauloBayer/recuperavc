package com.recuperavc.ui.main.reports.filters

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.theme.LocalReportsPalette
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun DateFilterCard(
    startDate: Instant,
    endDate: Instant,
    isManuallySet: Boolean,
    onStartDateChange: (Instant) -> Unit,
    onEndDateChange: (Instant) -> Unit,
    onTapSound: () -> Unit
) {
    val p = LocalReportsPalette.current
    val context = LocalContext.current

    // Read scale only (palette already comes from ReportsScreen provider)
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    val cardShape = RoundedCornerShape(16.dp)
    val tileShape = RoundedCornerShape(8.dp)

    val border = p.borderSoft?.let { BorderStroke(1.dp, it) }
    val tileBorderColor = p.borderSoft ?: Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = p.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (border != null) 0.dp else 4.dp),
        border = border
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Filtro de Data",
                fontWeight = FontWeight.Bold,
                fontSize = (18.sp * appliedScale),
                color = p.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            if (!isManuallySet) {
                Text(
                    text = "Mostrando dados dos últimos 7 dias",
                    fontSize = (14.sp * appliedScale),
                    color = p.textSecondary
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Início
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Início",
                        fontSize = (14.sp * appliedScale),
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(tileShape)
                            .background(p.surfaceVariant)
                            .border(width = 1.dp, color = tileBorderColor, shape = tileShape)
                            .clickable {
                                onTapSound()
                                val cal = Calendar.getInstance()
                                val ldt = LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())
                                cal.set(ldt.year, ldt.monthValue - 1, ldt.dayOfMonth)

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = LocalDateTime.of(year, month + 1, day, 0, 0)
                                        onStartDateChange(selected.atZone(ZoneId.systemDefault()).toInstant())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(
                                LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())
                            ),
                            fontSize = (16.sp * appliedScale),
                            fontWeight = FontWeight.Bold,
                            color = p.accent
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = p.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Fim
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Fim",
                        fontSize = (14.sp * appliedScale),
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(tileShape)
                            .background(p.surfaceVariant)
                            .border(width = 1.dp, color = tileBorderColor, shape = tileShape)
                            .clickable {
                                onTapSound()
                                val cal = Calendar.getInstance()
                                val ldt = LocalDateTime.ofInstant(endDate, ZoneId.systemDefault())
                                cal.set(ldt.year, ldt.monthValue - 1, ldt.dayOfMonth)

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                                        onEndDateChange(selected.atZone(ZoneId.systemDefault()).toInstant())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(
                                LocalDateTime.ofInstant(endDate, ZoneId.systemDefault())
                            ),
                            fontSize = (16.sp * appliedScale),
                            fontWeight = FontWeight.Bold,
                            color = p.accent
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = p.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
