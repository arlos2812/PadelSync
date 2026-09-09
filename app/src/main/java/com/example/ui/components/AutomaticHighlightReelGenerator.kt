package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HighlightReel
import com.example.viewmodel.PadelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomaticHighlightReelGeneratorCard(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isCompiling by viewModel.isCompilingReel.collectAsState()
    val compilationProgress by viewModel.reelCompilationProgress.collectAsState()
    val compilationStep by viewModel.reelCompilationStep.collectAsState()
    val latestReel by viewModel.latestCompiledReel.collectAsState()
    val generatedReels by viewModel.generatedReels.collectAsState()

    var showGeneratorDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf<HighlightReel?>(null) }

    // Dialog for creating a new reel
    if (showGeneratorDialog) {
        ReelConfigurationDialog(
            viewModel = viewModel,
            onDismiss = { showGeneratorDialog = false },
            onStarted = {
                showGeneratorDialog = false
            }
        )
    }

    // Modal player preview
    if (showPlayerDialog != null) {
        ReelPlayerDialog(
            reel = showPlayerDialog!!,
            onDismiss = { showPlayerDialog = null },
            onShare = { reel ->
                val shareText = """
                    🎾 ¡MIRA MI REEL AUTOMÁTICO DE PÁDEL! 🎬
                    🔥 Metraje: ${reel.matchTitle}
                    ⏱️ Duración: ${reel.durationSec} segundos • Formato: ${reel.aspectRatio}
                    🎵 Banda Sonora: ${reel.musicTrack}
                    ⭐ Jugadas destacadas incluidas:
                    ${reel.clipsIncluded.mapIndexed { idx, clip -> "${idx + 1}. $clip" }.joinToString("\n")}
                    
                    Compilado automáticamente con IA en Padel Tracker App.
                """.trimIndent()

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir Reel de Pádel"))
            },
            onSaveToPremier = { reel ->
                viewModel.saveReelToPremierHighlights(reel)
                showPlayerDialog = null
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automatic_reel_generator_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFE91E63))))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFF9800).copy(alpha = 0.14f),
                            Color(0xFFE91E63).copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFE91E63)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Reel Generator IA",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFF9800))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("NUEVO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Text(
                                text = "Compila los mejores puntos de partido en un clip corto",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                // If currently compiling
                AnimatedVisibility(visible = isCompiling) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Compilando metraje con IA...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${(compilationProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            }
                            LinearProgressIndicator(
                                progress = { compilationProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFFF9800),
                            )
                            Text(
                                text = compilationStep,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Action Call: Button to configure and generate new Reel
                if (!isCompiling) {
                    Button(
                        onClick = { showGeneratorDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_open_reel_generator"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎬 Generar Clip de Mejores Momentos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Previously Generated Reels Carousel
                if (generatedReels.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Clips Compilados Listos para Compartir",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(generatedReels, key = { it.id }) { reel ->
                                ReelMiniCard(
                                    reel = reel,
                                    onClick = { showPlayerDialog = reel }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReelMiniCard(
    reel: HighlightReel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("reel_card_${reel.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E24)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("⏱️ ${reel.durationSec}s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(reel.aspectRatio.take(4), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Text(
                text = reel.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = reel.matchTitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎵 ${reel.musicTrack.take(16)}...", fontSize = 9.sp, color = Color(0xFF81C784))
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Ver reel",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelConfigurationDialog(
    viewModel: PadelViewModel,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    val matches = listOf(
        "Coello/Tapia vs Chingotto/Galán (Final Qatar Major)",
        "Lebrón/Galán vs Stupaczuk/Di Nenno (Madrid P1)",
        "Mi Último Partido Jugado (Historial Local)",
        "Metraje Grabado en Pista Central"
    )

    var selectedMatch by remember { mutableStateOf(matches[0]) }
    var selectedDuration by remember { mutableStateOf(45) }
    var selectedRatio by remember { mutableStateOf("9:16 (Reels/TikTok)") }
    var selectedMusic by remember { mutableStateOf("Premier Stadium Electro Beat") }

    val criteriaOptions = listOf(
        "Puntazos > 25 golpes",
        "Remates x3 ganadores",
        "Salidas de pista épicas",
        "Puntos de quiebre y Match Ball",
        "Bloqueos reflejos en la red"
    )
    val selectedCriteria = remember { mutableStateListOf("Puntazos > 25 golpes", "Remates x3 ganadores", "Puntos de quiebre y Match Ball") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.compileHighlightReel(
                        matchTitle = selectedMatch,
                        durationSec = selectedDuration,
                        aspectRatio = selectedRatio,
                        selectedCriteria = selectedCriteria.toList(),
                        musicTrack = selectedMusic
                    )
                    onStarted()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compilar Reel Ahora", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF9800))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generador Automático de Reel", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Select Match Footage
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("1. Metraje de Partido:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    matches.forEach { matchOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMatch = matchOption }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMatch == matchOption,
                                onClick = { selectedMatch = matchOption }
                            )
                            Text(matchOption, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Select Criteria
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("2. Criterios de Selección IA (Puntazos Clave):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    criteriaOptions.forEach { crit ->
                        val isChecked = selectedCriteria.contains(crit)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) selectedCriteria.remove(crit) else selectedCriteria.add(crit)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedCriteria.add(crit) else selectedCriteria.remove(crit)
                                }
                            )
                            Text(crit, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Ratio & Duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("3. Formato y Duración del Clip:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60).forEach { dur ->
                            FilterChip(
                                selected = selectedDuration == dur,
                                onClick = { selectedDuration = dur },
                                label = { Text("${dur}s", fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("9:16 (Reels/TikTok)", "16:9 (Padel TV)").forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ReelPlayerDialog(
    reel: HighlightReel,
    onDismiss: () -> Unit,
    onShare: (HighlightReel) -> Unit,
    onSaveToPremier: (HighlightReel) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentClipIndex by remember { mutableStateOf(0) }
    var playbackProgress by remember { mutableFloatStateOf(0.35f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val audioPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio_pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onShare(reel) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir Clip")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSaveToPremier(reel) }) {
                    Text("Añadir a 'Lo Mejor'")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFFF9800))
                Spacer(modifier = Modifier.width(8.dp))
                Text(reel.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simulated High-end Video Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    // Court background simulation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Watermark HUD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color.Red else Color.Gray)
                                    )
                                    Text("REEL IA • ${reel.aspectRatio.take(4)}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF9800))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PADEL SYNC", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        // Center Play / Pause Indicator
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Pausado",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        // Bottom HUD Overlay: Current Play Title & Progress
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔥 ${reel.clipsIncluded.getOrNull(currentClipIndex) ?: reel.clipsIncluded.firstOrNull() ?: "Punto Clave"}",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { playbackProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFFFF9800),
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // Audio & Metadata info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = reel.musicTrack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = "⏱️ ${reel.durationSec}s • ${reel.selectedPointsCount} jugadas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
