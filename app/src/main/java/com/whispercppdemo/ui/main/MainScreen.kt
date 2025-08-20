package com.whispercppdemo.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.whispercppdemo.R

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    MainScreen(
        canTranscribe = viewModel.canTranscribe,
        isRecording = viewModel.isRecording,
        isLoading = viewModel.isLoading,
        isProcessing = viewModel.isProcessing,
        transcriptionResult = viewModel.transcriptionResult,
        analysisResult = viewModel.analysisResult,
        onRecordTapped = viewModel::toggleRecord
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    canTranscribe: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    isProcessing: Boolean,
    transcriptionResult: String,
    analysisResult: AnalysisResult?,
    onRecordTapped: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF1F8E9)
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                // Loading State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = Color(0xFF43A047),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Carregando modelo de IA...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF424242),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Main Content
                // Instruction Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF81C784)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.instruction_text),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.phrase_to_read),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Recording Button
                RecordButton(
                    enabled = canTranscribe && !isProcessing,
                    isRecording = isRecording,
                    isProcessing = isProcessing,
                    onClick = onRecordTapped
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Processing Indicator
                if (isProcessing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color(0xFF43A047),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Processando áudio...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Results Section
                if ((transcriptionResult.isNotEmpty() || analysisResult != null) && !isProcessing) {
                    ResultsSection(transcriptionResult, analysisResult)
                }
            }
        }
    }
}

@Composable
private fun ResultsSection(transcriptionResult: String, analysisResult: AnalysisResult?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.results_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (transcriptionResult.isNotEmpty()) {
                Text(
                    text = "Texto transcrito:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = transcriptionResult,
                    fontSize = 16.sp,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            analysisResult?.let { result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wpm_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "${result.wpm}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                result.wpm >= 120 -> Color(0xFF388E3C)  // Verde: Normal (120+ WPM)
                                result.wpm >= 60 -> Color(0xFFF57C00)   // Laranja: Atenção (60-119 WPM)
                                else -> Color(0xFFD32F2F)               // Vermelho: Preocupante (<60 WPM)
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wer_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "${String.format("%.1f", result.wer)}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                result.wer <= 10 -> Color(0xFF388E3C)  // Verde: Normal
                                result.wer <= 20 -> Color(0xFFF57C00)  // Laranja: Atenção  
                                else -> Color(0xFFD32F2F)              // Vermelho: Preocupante
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.wer <= 10 && result.wpm >= 120 -> Color(0xFFE8F5E8)    // Verde: Normal
                            result.wer <= 20 && result.wpm >= 60 -> Color(0xFFFFF3E0)     // Laranja: Atenção
                            else -> Color(0xFFFFEBEE)                                      // Vermelho: Preocupante
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            result.wer <= 10 && result.wpm >= 120 -> stringResource(R.string.assessment_normal)
                            result.wer <= 20 && result.wpm >= 60 -> stringResource(R.string.assessment_attention)
                            else -> stringResource(R.string.assessment_concern)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            result.wer <= 10 && result.wpm >= 120 -> Color(0xFF2E7D32)   // Verde: Normal
                            result.wer <= 20 && result.wpm >= 60 -> Color(0xFFE65100)    // Laranja: Atenção
                            else -> Color(0xFFC62828)                                     // Vermelho: Preocupante
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RecordButton(enabled: Boolean, isRecording: Boolean, isProcessing: Boolean, onClick: () -> Unit) {
    val micPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { granted ->
            if (granted) {
                onClick()
            }
        }
    )
    
    FloatingActionButton(
        onClick = {
            if (micPermissionState.status.isGranted) {
                onClick()
            } else {
                micPermissionState.launchPermissionRequest()
            }
        },
        modifier = Modifier.size(80.dp),
        containerColor = when {
            !enabled -> Color(0xFF9E9E9E) // Gray when disabled
            isRecording -> Color(0xFFE53935) // Red when recording
            else -> Color(0xFF43A047) // Green when ready
        },
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = when {
                isProcessing -> "⏳"
                isRecording -> "⏹"
                else -> "🎤"
            },
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = when {
            isProcessing -> "Processando..."
            isRecording -> stringResource(R.string.stop_recording)
            else -> stringResource(R.string.start_recording)
        },
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = when {
            !enabled -> Color(0xFF9E9E9E)
            isRecording -> Color(0xFFE53935)
            else -> Color(0xFF43A047)
        },
        textAlign = TextAlign.Center
    )
}

