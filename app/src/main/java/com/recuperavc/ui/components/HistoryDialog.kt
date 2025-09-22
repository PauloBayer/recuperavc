package com.recuperavc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.data.CurrentUser
import com.recuperavc.data.db.DbProvider
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.OnBackground

@Composable
fun HistoryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }
    val audioFiles by db.audioFileDao().observeForUser(CurrentUser.ID).collectAsState(initial = emptyList())
    val audioReports by db.audioReportDao().observeForUser(CurrentUser.ID).collectAsState(initial = emptyList())
    val coherenceReports by db.coherenceReportDao().observeForUser(CurrentUser.ID).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) { Text("Fechar", color = Color.White) }
        },
        title = { Text("Registros Salvos", color = Color.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("Arquivos de Áudio (${audioFiles.size})", fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(Modifier.height(8.dp))
                audioFiles.take(10).forEach {
                    val local = java.time.LocalDateTime.ofInstant(it.recordedAt, java.time.ZoneId.systemDefault())
                    val formatted = local.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    Text("• ${it.fileName} • ${formatted}", fontSize = 12.sp, color = Color.Black)
                }
                Spacer(Modifier.height(16.dp))
                Text("Relatórios de Áudio (${audioReports.size})", fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(Modifier.height(8.dp))
                audioReports.take(10).forEach {
                    Text("• ${it.allTestsDescription}", fontSize = 12.sp, color = Color.Black)
                }
                Spacer(Modifier.height(16.dp))
                Text("Relatórios de Coerência (${coherenceReports.size})", fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(Modifier.height(8.dp))
                coherenceReports.take(10).forEach {
                    Text("• score=${it.averageTimePerTry} • ${it.allTestsDescription}", fontSize = 12.sp, color = Color.Black)
                }
            }
        },
        containerColor = Color.White
    )
}

