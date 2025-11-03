// app/src/main/java/com/recuperavc/ui/main/export/ExportPdfButton.kt
package com.recuperavc.ui.main.export

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.MotionReport
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.main.reports.type.ReportTab
import com.recuperavc.ui.theme.LocalReportsPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun ExportPdfButton(
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant,
    audioReports: List<AudioReportWithFiles>,
    coherenceReports: List<CoherenceReport>,
    motionReports: List<MotionReport>,
    onTapSound: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = LocalReportsPalette.current

    // runtime permissions
    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    // pick readable content color for whatever accent the palette gives us
    val contentOnAccent = if (p.accent.luminance() > 0.6f) Color.Black else Color.White

    Button(
        onClick = {
            onTapSound?.invoke()

            // Legacy storage permission (< Q)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                    storageLauncher.launch(perm)
                    return@Button
                }
            }
            // Tiramisu+ notifications (optional, for “saved” notification)
            if (Build.VERSION.SDK_INT >= 33) {
                val perm = Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                    notifLauncher.launch(perm)
                }
            }

            scope.launch(Dispatchers.IO) {
                val uri = exportReportsToPdf(ctx, tab, startDate, endDate, audioReports, coherenceReports, motionReports)
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val fileName = buildReportFileName(tab, startDate, endDate)
                        com.recuperavc.util.ExportNotification.notifyPdfSaved(ctx, uri, fileName)
                        Toast.makeText(ctx, "PDF salvo em Downloads/RecuperAVC.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, "Falha ao salvar PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = contentOnAccent),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Filled.Description, contentDescription = null, tint = contentOnAccent)
        Spacer(Modifier.width(8.dp))
        Text("Exportar PDF", color = contentOnAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
