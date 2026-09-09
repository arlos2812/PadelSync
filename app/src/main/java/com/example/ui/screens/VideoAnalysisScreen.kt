package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.util.PadelChatMessage

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoAnalysis
import com.example.viewmodel.PadelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoAnalysisScreen(viewModel: PadelViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val allAnalyses by viewModel.allAnalyses.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingVideo.collectAsState()
    val stepText by viewModel.analysisStepText.collectAsState()
    val selectedVideoUri by viewModel.selectedVideoUri.collectAsState()
    val selectedSampleKey by viewModel.selectedSampleKey.collectAsState()
    val selectedStrokeHint by viewModel.selectedStrokeHint.collectAsState()
    val publishedAnalysisIds by viewModel.publishedAnalysisIds.collectAsState()

    var showShareFriendsDialog by remember { mutableStateOf(false) }
    var showPublishCommunityDialog by remember { mutableStateOf(false) }
    var currentCoachTab by remember { mutableStateOf(0) } // 0: Video Analysis, 1: AI Chat Coach

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    // Android 13+ Photo/Video Picker (Zero-permission, Google Play compliant)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideoUri(uri)
        }
    }

    // Camera Video Recording Integration (FileProvider based)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { isSaved: Boolean ->
        if (isSaved && tempCameraUri != null) {
            viewModel.selectVideoUri(tempCameraUri!!)
            Toast.makeText(context, "Vídeo grabado con cámara listo para análisis", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraRecord = {
        try {
            val videoFile = File(context.cacheDir, "padel_swing_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
            tempCameraUri = uri
            cameraRecordLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo iniciar la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val demoClips = listOf(
        Triple("bandeja", "🎾 Bandeja a la Reja", "Impacto y corte"),
        Triple("remate", "💥 Remate x3 Metros", "Salida de pista"),
        Triple("vibora", "🐍 Víbora al Rincón", "Slice y doble pared"),
        Triple("bajada", "🛡️ Bajada de Pared", "Poder desde el fondo"),
        Triple("volea", "⚡ Volea de Red", "Bloqueo y anticipación")
    )

    val strokeHints = listOf("Auto-detectar", "Bandeja", "Víbora", "Remate x3", "Bajada de Pared", "Volea")

    // Pulse animation for AI loading
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ai")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Header Hero Banner ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_video_hero"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Coach de Pádel con IA",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Análisis de vídeo con Gemini 3.5 Flash",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Sube o graba un vídeo de tu golpe o elige una jugada demo. La Inteligencia Artificial analizará tu técnica, calculará tu puntuación del 1 al 100 y te detallará exactamente qué aspectos debes mejorar.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Coach Tab Switcher: Video Analysis vs Gemini Chat Coach
        TabRow(
            selectedTabIndex = currentCoachTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = currentCoachTab == 0,
                onClick = { currentCoachTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Análisis Biomecánico", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = currentCoachTab == 1,
                onClick = { currentCoachTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Chat Coach IA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (currentCoachTab == 1) {
            // Coach Gemini Chatbot Screen
            CoachChatbotSection(
                messages = chatMessages,
                isLoading = isChatLoading,
                currentAnalysis = currentAnalysis,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onClearChat = { viewModel.clearChat() }
            )
        } else {
        // --- Video Selection Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_video_input"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "1. Graba con Cámara o Selecciona Vídeo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Row of Actions: Camera Record & Gallery Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Record with Camera Button
                    Button(
                        onClick = launchCameraRecord,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("btn_record_camera_video"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grabar Cámara",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Native Gallery Video Picker Button
                    OutlinedButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("btn_pick_gallery_video"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedVideoUri != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedVideoUri != null) "✓ Galería" else "Desde Galería",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Camera recording recommendation banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Consejo de grabación: Graba 5 a 10 segundos en plano lateral o 45° a 3 metros. Haz 2 repeticiones de tu swing (bandeja, víbora o remate) para evaluar técnica y consistencia con Gemini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // If user selected a local video Uri
                if (selectedVideoUri != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Vídeo Personal Listo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Fotogramas listos para analizar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = { viewModel.selectSample("bandeja") },
                                modifier = Modifier.testTag("btn_clear_custom_video")
                            ) {
                                Text("Usar Demo", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Demo Clips Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "O prueba con una jugada demo de ejemplo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(demoClips) { clip ->
                            val isSelected = selectedSampleKey == clip.first && selectedVideoUri == null
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.selectSample(clip.first) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("demo_clip_${clip.first}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = clip.second,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = clip.third,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Stroke Type Hint Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. Tipo de Golpe a Analizar:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(strokeHints) { hint ->
                            FilterChip(
                                selected = selectedStrokeHint == hint,
                                onClick = { viewModel.setStrokeHint(hint) },
                                label = { Text(hint, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Main Analysis Button
                Button(
                    onClick = { viewModel.analyzeCurrentVideo() },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_analyze_video"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnalyzing) "Analizando Golpe..." else "Analizar Técnica y Obtener Puntuación",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- Analyzing Progress Card ---
        AnimatedVisibility(
            visible = isAnalyzing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_analyzing_progress"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stepText.ifBlank { "Analizando técnica con IA..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- Current Analysis Result Display ---
        if (currentAnalysis != null && !isAnalyzing) {
            val analysis = currentAnalysis!!
            val isPublished = analysis.id in publishedAnalysisIds
            AnalysisResultCard(
                analysis = analysis,
                isPublished = isPublished,
                onSpeak = {
                    viewModel.speakFeedback("Puntuación técnica: ${analysis.score} sobre 100. Nivel: ${analysis.levelTier}. Aspectos a mejorar: ${analysis.improvementFeedback}")
                },
                onShare = {
                    val shareText = """
                        🎾 ANÁLISIS DE VÍDEO PADEL COACH IA
                        Golpe: ${analysis.strokeType}
                        Puntuación: ${analysis.score}/100 ⭐
                        Nivel: ${analysis.levelTier}
                        
                        💡 Aspectos a Mejorar:
                        ${analysis.improvementFeedback}
                        
                        💪 Puntos Fuertes:
                        ${analysis.positiveFeedback}
                        
                        🎯 Ejercicio Recomendado:
                        ${analysis.recommendedDrill}
                        
                        Analizado con PadelSync AI Coach
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Análisis de Pádel - ${analysis.strokeType}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir informe técnico"))
                },
                onAskCoach = {
                    currentCoachTab = 1
                    viewModel.askAboutCurrentAnalysis(analysis)
                },
                onShareWithFriends = { showShareFriendsDialog = true },
                onPublishCommunity = { showPublishCommunityDialog = true },
                onPublishToProfile = {
                    viewModel.shareAnalysisToPublicProfile(analysis)
                    Toast.makeText(context, "✅ Jugada fijada en tu perfil público", Toast.LENGTH_SHORT).show()
                },
                onCopyLink = {
                    val link = "padelsync://analysis/${analysis.id}"
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("PlayLink", link))
                    Toast.makeText(context, "🔗 Enlace copiado: $link", Toast.LENGTH_SHORT).show()
                },
                onDelete = { viewModel.deleteAnalysis(analysis.id) }
            )
        }

        // --- Dialogs for Sharing with Friends & Publishing to Community ---
        if (showShareFriendsDialog && currentAnalysis != null) {
            ShareAnalysisWithFriendsDialog(
                analysis = currentAnalysis!!,
                onDismiss = { showShareFriendsDialog = false },
                onSend = { selectedFriends, message ->
                    viewModel.sendAnalysisLinkToFriends(currentAnalysis!!, selectedFriends, message)
                    showShareFriendsDialog = false
                },
                onExternalShare = {
                    val shareText = """
                        🎾 ¡MIRA MI JUGADA EN PADELSYNC!
                        Golpe: ${currentAnalysis!!.strokeType}
                        Puntuación: ${currentAnalysis!!.score}/100 ⭐ (${currentAnalysis!!.levelTier})
                        
                        ✅ Lo que está bien:
                        ${currentAnalysis!!.positiveFeedback}
                        
                        ⚠️ Cosas a mejorar:
                        ${currentAnalysis!!.improvementFeedback}
                        
                        🎯 Ejercicio:
                        ${currentAnalysis!!.recommendedDrill}
                    """.trimIndent()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Mi jugada de pádel: ${currentAnalysis!!.strokeType}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir con amigos"))
                }
            )
        }

        if (showPublishCommunityDialog && currentAnalysis != null) {
            PublishAnalysisToCommunityDialog(
                analysis = currentAnalysis!!,
                onDismiss = { showPublishCommunityDialog = false },
                onPublish = { caption ->
                    viewModel.publishAnalysisToCommunityFeed(currentAnalysis!!, caption, shareToProfile = true)
                    viewModel.publishVideoAnalysisToCommunity(currentAnalysis!!, caption)
                    showPublishCommunityDialog = false
                }
            )
        }

        // --- Historical Analyses Section ---
        if (allAnalyses.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Historial de Vídeos Analizados (${allAnalyses.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                allAnalyses.forEach { histAnalysis ->
                    HistoryAnalysisItem(
                        analysis = histAnalysis,
                        isSelected = currentAnalysis?.id == histAnalysis.id,
                        onSelect = { viewModel.setCurrentAnalysis(histAnalysis) },
                        onDelete = { viewModel.deleteAnalysis(histAnalysis.id) }
                    )
                }
            }
        }
        } // End of else branch (currentCoachTab == 0)
    }
}

@Composable
fun AnalysisResultCard(
    analysis: VideoAnalysis,
    isPublished: Boolean = false,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onAskCoach: () -> Unit = {},
    onShareWithFriends: () -> Unit,
    onPublishCommunity: () -> Unit,
    onPublishToProfile: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onDelete: () -> Unit
) {
    val scoreColor = when {
        analysis.score >= 85 -> Color(0xFF4CAF50)
        analysis.score >= 70 -> Color(0xFF2196F3)
        analysis.score >= 55 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_analysis_result")
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header with stroke name and share / audio actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analysis.strokeType,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = analysis.levelTier,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.testTag("btn_speak_analysis")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Escuchar feedback",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("btn_share_analysis")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir resultado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Central Score Badge
            Surface(
                color = scoreColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PUNTUACIÓN TÉCNICA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = scoreColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${analysis.score} / 100",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                            text = when {
                                analysis.score >= 85 -> "Nivel Excelente / Pro"
                                analysis.score >= 70 -> "Nivel Avanzado Competitivo"
                                analysis.score >= 55 -> "Nivel Intermedio"
                                else -> "Nivel En Formación"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(scoreColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Sub-metrics Progress Bars
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Métricas Técnicas Específicas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                MetricProgressBar(title = "Juego de Pies y Posicionamiento", score = analysis.footworkScore)
                MetricProgressBar(title = "Punto de Impacto y Altura", score = analysis.impactScore)
                MetricProgressBar(title = "Terminación y Acompañamiento", score = analysis.followThroughScore)
                MetricProgressBar(title = "Táctica y Profundidad", score = analysis.tacticsScore)
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // KEY SECTION: QUÉ SE PUEDE MEJORAR (As requested by user)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_improvements"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "QUÉ SE PUEDE MEJORAR (Correcciones)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = analysis.improvementFeedback,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )
                }
            }

            // PUNTOS FUERTES
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_strengths"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Puntos Fuertes Detectados",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Text(
                        text = analysis.positiveFeedback,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            // EJERCICIO / DRILL RECOMENDADO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_recommended_drill"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ejercicio Recomendado para la Pista",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = analysis.recommendedDrill,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            // Preguntar al Coach con IA (Gemini Chatbot) Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAskCoach() }
                    .testTag("card_ask_coach_analysis"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "💬 Preguntar al Coach Gemini",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "¿Dudas con la corrección biomecánica? Chatea con la IA",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onAskCoach,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Chatear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Status Badge if already published
            if (isPublished) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Publicado en la Comunidad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Visible para todos los jugadores en 'Lo Mejor'",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Share With Friends and Publish To All Actions
            Text(
                text = "Difundir y Compartir Jugada",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Friends + Public Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Share With Friends (Enlace)
                    Button(
                        onClick = onShareWithFriends,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_with_friends"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Enviar a Amigos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button 2: Publish to Public Profile
                    Button(
                        onClick = onPublishToProfile,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_publish_to_profile"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Perfil Público", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Row 2: Community Feed + Direct Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 3: Publish in Community Feed
                    Button(
                        onClick = onPublishCommunity,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("btn_publish_to_all"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPublished) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (isPublished) "Publicado en Feed" else "Publicar en Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button 4: Copy Direct Link
                    OutlinedButton(
                        onClick = onCopyLink,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("btn_copy_play_link"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Copiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricProgressBar(title: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$score%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                score >= 85 -> Color(0xFF4CAF50)
                score >= 70 -> Color(0xFF2196F3)
                else -> Color(0xFFFF9800)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun HistoryAnalysisItem(
    analysis: VideoAnalysis,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(Date(analysis.timestamp))
    val scoreColor = when {
        analysis.score >= 85 -> Color(0xFF4CAF50)
        analysis.score >= 70 -> Color(0xFF2196F3)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_item_${analysis.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scoreColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${analysis.score}",
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor,
                        fontSize = 15.sp
                    )
                }

                Column {
                    Text(
                        text = analysis.strokeType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$dateStr • ${analysis.levelTier}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("btn_delete_analysis_${analysis.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ShareAnalysisWithFriendsDialog(
    analysis: VideoAnalysis,
    onDismiss: () -> Unit,
    onSend: (List<String>, String) -> Unit,
    onExternalShare: () -> Unit
) {
    val sampleFriends = listOf(
        Pair("Carlos Mendoza", "Nivel 4.5 • Drive"),
        Pair("Lucía Ruiz", "Nivel 3.8 • Revés"),
        Pair("Andrés Gómez", "Nivel 4.0 • Drive"),
        Pair("Marta Serrano", "Nivel 4.2 • Rematadora"),
        Pair("Javier Soto", "Nivel 3.5 • Defensivo")
    )
    val selectedFriends = remember { mutableStateListOf("Carlos Mendoza", "Lucía Ruiz") }
    var userMessage by remember { mutableStateOf("¡Mirad este golpe de ${analysis.strokeType}! ¿Qué opináis de mi técnica?") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Compartir con Amigos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Analysis preview mini pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "🎾 ${analysis.strokeType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Nivel: ${analysis.levelTier}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${analysis.score}/100 ⭐",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Selecciona con qué amigos compartir:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Friends checklist
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sampleFriends.forEach { (name, subtitle) ->
                        val isChecked = selectedFriends.contains(name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) selectedFriends.remove(name) else selectedFriends.add(name)
                                }
                                .background(if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Column {
                                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFriends.add(name) else selectedFriends.remove(name)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    label = { Text("Mensaje para tus amigos", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedButton(
                    onClick = onExternalShare,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir por WhatsApp u otras Apps", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(selectedFriends.toList(), userMessage) },
                enabled = selectedFriends.isNotEmpty(),
                modifier = Modifier.testTag("btn_send_to_friends_confirm"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar (${selectedFriends.size})", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun PublishAnalysisToCommunityDialog(
    analysis: VideoAnalysis,
    onDismiss: () -> Unit,
    onPublish: (String) -> Unit
) {
    var caption by remember { mutableStateOf("¡Nuevo golpe analizado por IA! Puntuación ${analysis.score}/100 ⭐. ¿Qué os parece?") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Publicar para Todos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tu jugada se publicará en la pestaña 'Lo Mejor' y el muro social para que todos los usuarios puedan ver tu golpe, tu puntuación y aprender de tus correcciones técnicas.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )

                // Card Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 Jugada: ${analysis.strokeType}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    text = "${analysis.score}/100 ⭐",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "✅ Aciertos: ${analysis.positiveFeedback.take(80)}...",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "💡 A Mejorar: ${analysis.improvementFeedback.take(80)}...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Comentario para la comunidad", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(caption) },
                modifier = Modifier.testTag("btn_confirm_publish_community"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publicar Ahora", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun CoachChatbotSection(
    messages: List<PadelChatMessage>,
    isLoading: Boolean,
    currentAnalysis: com.example.model.VideoAnalysis?,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "¿Cómo mejorar el punto de impacto en la bandeja?",
        "¿Qué ejercicio puedo hacer para el remate por 3?",
        "¿Cómo posicionar los pies en la bajada de pared?",
        "Consejos tácticos para jugar con rivales agresivos"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coach_chat_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chat Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Asesor Biomecánico Gemini",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentAnalysis != null) "Contexto: ${currentAnalysis.strokeType} (${currentAnalysis.score}/100)" else "IA lista para responder sobre pádel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar conversación",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick suggested questions
        Text(
            text = "Preguntas rápidas al entrenador:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickQuestions) { question ->
                SuggestionChip(
                    onClick = { onSendMessage(question) },
                    label = { Text(question, fontSize = 11.sp, maxLines = 1) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Chat message history list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 460.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                messages.forEach { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isUser) 14.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 14.dp
                            ),
                            color = if (isUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isUser) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Coach Gemini",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "El Coach está analizando...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_coach_chat"),
                placeholder = { Text("Pregunta sobre técnica, táctica...", fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp),
                maxLines = 3,
                singleLine = false
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText
                        inputText = ""
                        onSendMessage(textToSend)
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("btn_send_coach_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar pregunta",
                    tint = if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


