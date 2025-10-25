package com.recuperavc.ui.main

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.R
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.theme.OnBackground
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import com.recuperavc.ui.main.components.SegmentedTabs
import com.recuperavc.ui.main.filters.DateFilterCard
import com.recuperavc.ui.main.audio.AudioReportSection
import com.recuperavc.ui.main.audio.AudioReportDetailDialog
import com.recuperavc.ui.main.audio.parseAudioReportDetails
import com.recuperavc.ui.main.motion.MotionReportSection
import com.recuperavc.ui.main.coherence.CoherenceReportSection
import com.recuperavc.ui.main.coherence.CoherenceReportDetailDialog
import com.recuperavc.ui.main.coherence.parseCoherenceReportGroups
import com.recuperavc.ui.main.export.ExportPdfButton
import com.recuperavc.ui.main.ReportTab
import com.recuperavc.ui.main.ChartType
import com.recuperavc.ui.main.CoherenceChartType

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }
    val sfx = rememberSfxController()

    var tab by remember { mutableStateOf(ReportTab.Audio) }
    var selectedAudioReport by remember { mutableStateOf<Pair<AudioReportWithFiles, ChartType>?>(null) }
    var selectedCoherenceReport by remember { mutableStateOf<Pair<com.recuperavc.models.CoherenceReport, CoherenceChartType>?>(null) }

    val now = remember { Instant.now() }
    val defaultStartDate = remember { now.minus(7, ChronoUnit.DAYS) }
    var startDate by remember { mutableStateOf(defaultStartDate) }
    var endDate by remember { mutableStateOf(now) }
    var isDateManuallySet by remember { mutableStateOf(false) }

    val notifPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { }

    val rawAudioReports by db.audioReportDao()
        .observeAllWithFiles()
        .map { list ->
            list.filter { parseAudioReportDetails(it.report.allTestsDescription).isNotEmpty() }
        }
        .collectAsState(initial = emptyList())

    val audioReports = remember(rawAudioReports, startDate, endDate) {
        rawAudioReports
            .filter { rep ->
                val minDate = rep.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
                minDate >= startDate && minDate <= endDate
            }
            .sortedBy { rep ->
                rep.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
            }
    }

    val rawMotionReports by db.MotionReportDao()
        .observeAll()
        .map { it.sortedBy { r -> r.date } }
        .collectAsState(initial = emptyList())

    val motionReports = remember(rawMotionReports, startDate, endDate) {
        rawMotionReports.filter { it.date >= startDate && it.date <= endDate }
    }

    val coherenceReports by db.coherenceReportDao()
        .observeAll()
        .collectAsState(initial = emptyList())

    val filteredCoherenceReports = remember(coherenceReports, startDate, endDate) {
        coherenceReports
            .filter { r ->
                val groups = parseCoherenceReportGroups(r.allTestsDescription)
                groups.isNotEmpty() && r.date >= startDate && r.date <= endDate
            }
            .sortedBy { it.date }
    }

    var phrases by remember { mutableStateOf<List<com.recuperavc.models.Phrase>>(emptyList()) }
    LaunchedEffect(Unit) {
        phrases = db.phraseDao().getAll()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.wave_green),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = { sfx.play(Sfx.CLICK); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnBackground)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Relatórios",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                SegmentedTabs(
                    tab = tab,
                    onTab = {
                        sfx.play(Sfx.CLICK)
                        tab = it
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DateFilterCard(
                    startDate = startDate,
                    endDate = endDate,
                    isManuallySet = isDateManuallySet,
                    onStartDateChange = {
                        startDate = it
                        isDateManuallySet = true
                    },
                    onEndDateChange = {
                        endDate = it
                        isDateManuallySet = true
                    },
                    onTapSound = { sfx.play(Sfx.CLICK) }
                )
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    ReportTab.Audio -> AudioReportSection(
                        items = audioReports,
                        onSelectReport = { report, chartType -> selectedAudioReport = report to chartType },
                        onBarTapSound = { sfx.play(Sfx.CLICK) }
                    )
                    ReportTab.Coherence -> CoherenceReportSection(
                        items = filteredCoherenceReports,
                        onSelectReport = { report, chartType -> selectedCoherenceReport = report to chartType },
                        onBarTapSound = { sfx.play(Sfx.CLICK) }
                    )
                    ReportTab.Motion -> MotionReportSection(motionReports)
                }

                Spacer(Modifier.height(20.dp))
                ExportPdfButton(
                    tab = tab,
                    startDate = startDate,
                    endDate = endDate,
                    audioReports = audioReports,
                    coherenceReports = filteredCoherenceReports,
                    motionReports = motionReports,
                    onTapSound = { sfx.play(Sfx.CLICK) }
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        selectedAudioReport?.let { (rep, chartType) ->
            AudioReportDetailDialog(
                report = rep,
                chartType = chartType,
                onDismiss = { selectedAudioReport = null },
                onAnyTap = { sfx.play(Sfx.CLICK) }
            )
        }

        selectedCoherenceReport?.let { (rep, chartType) ->
            CoherenceReportDetailDialog(
                report = rep,
                chartType = chartType,
                phraseMap = remember(phrases) { phrases.associateBy { it.id } },
                onDismiss = { selectedCoherenceReport = null },
                onAnyTap = { sfx.play(Sfx.CLICK) }
            )
        }
    }
}
 
