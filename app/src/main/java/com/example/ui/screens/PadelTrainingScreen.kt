package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadelDrillSession
import com.example.viewmodel.PadelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DrillCategory(
    val id: String,
    val name: String,
    val defaultReps: Int,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
    val focusCue: String
)

/**
 * Screen for Padel Training Mode:
 * Allows players to track, count, and log specific drill series (e.g., volleys, bandejas, smashes)
 * without needing to play a full match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadelTrainingScreen(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val drills by viewModel.allDrills.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val drillCategories = remember {
        listOf(
            DrillCategory(
                id = "voleas",
                name = "Voleas en Red",
                defaultReps = 30,
                description = "Series de volea de derecha y revés a velocidad media y alta.",
                icon = Icons.Default.SportsTennis,
                accentColor = Color(0xFF2196F3),
                focusCue = "Mantén la pala alta por encima de la cintura y paso firme al frente."
            ),
            DrillCategory(
                id = "bandejas",
                name = "Bandejas y Víboras",
                defaultReps = 25,
                description = "Control del rebote hacia la reja lateral rival y profundidad.",
                icon = Icons.Default.Cyclone,
                accentColor = Color(0xFFFF9800),
                focusCue = "Punto de impacto a las 2 en punto con terminación hacia el bolsillo contrario."
            ),
            DrillCategory(
                id = "remates",
                name = "Remates x3 / x4",
                defaultReps = 20,
                description = "Potencia, arco de flexión y salida de bola de la pista.",
                icon = Icons.Default.FlashOn,
                accentColor = Color(0xFFFF5722),
                focusCue = "Acelera el brazo rápido sobre la cabeza arqueando la espalda."
            ),
            DrillCategory(
                id = "bajadas",
                name = "Bajadas de Pared",
                defaultReps = 25,
                description = "Aceleración de arriba hacia abajo tras rebote alto en cristal.",
                icon = Icons.Default.VerticalAlignBottom,
                accentColor = Color(0xFF9C27B0),
                focusCue = "Entra decidido al rebote y golpea la bola delante del cuerpo."
            ),
            DrillCategory(
                id = "saques",
                name = "Saques de Precisión",
                defaultReps = 30,
                description = "Bote en línea, dirección a la T y apertura hacia la pared lateral.",
                icon = Icons.Default.GpsFixed,
                accentColor = Color(0xFF00B0FF),
                focusCue = "Bote a la altura de la cintura con peso transferido hacia la red."
            ),
            DrillCategory(
                id = "globos",
                name = "Defensa y Globos",
                defaultReps = 30,
                description = "Globos altos milimétricos para recuperar la red tras cristal.",
                icon = Icons.Default.CloudUpload,
                accentColor = Color(0xFF4CAF50),
                focusCue = "Flexiona rodillas bien bajo la bola y empuja hacia el cielo."
            )
        )
    }

    var selectedCategory by remember { mutableStateOf(drillCategories[0]) }
    var targetReps by remember { mutableStateOf(selectedCategory.defaultReps) }
    var successfulReps by remember { mutableStateOf(0) }
    var failedReps by remember { mutableStateOf(0) }
    var difficultyLevel by remember { mutableStateOf("Intermedio") }
    var technicalNotes by remember { mutableStateOf("") }

    // Live drill timer
    var isTimerRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    // History filter
    var historyFilter by remember { mutableStateOf("Todos") }

    // Training summary stats
    val totalSessions = drills.size
    val totalRepsCount = drills.sumOf { it.targetReps }
    val totalSuccessCount = drills.sumOf { it.successfulReps }
    val globalAccuracy = if (totalRepsCount > 0) (totalSuccessCount.toFloat() / totalRepsCount.toFloat()) * 100f else 0f
    val totalMinutes = drills.sumOf { it.durationMinutes }

    val totalCompletedReps = successfulReps + failedReps
    val currentAccuracy = if (totalCompletedReps > 0) {
        (successfulReps.toFloat() / totalCompletedReps.toFloat()) * 100f
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("padel_training_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Entrenamiento de Series",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Perfecciona golpes técnicos sin jugar partido completo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 2. Global Training Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Series", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("$totalSessions", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("Registradas", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Precisión", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = if (totalSessions > 0) "${globalAccuracy.toInt()}%" else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676)
                        )
                        Text("Media de aciertos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Tiempo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${totalMinutes}m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF9800))
                        Text("En pista", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // 3. Drill Type Category Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Selecciona Tipo de Ejercicio Técnico:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    drillCategories.take(3).forEach { cat ->
                        val isSelected = selectedCategory.id == cat.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) cat.accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    selectedCategory = cat
                                    targetReps = cat.defaultReps
                                    successfulReps = 0
                                    failedReps = 0
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = cat.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    drillCategories.drop(3).forEach { cat ->
                        val isSelected = selectedCategory.id == cat.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) cat.accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    selectedCategory = cat
                                    targetReps = cat.defaultReps
                                    successfulReps = 0
                                    failedReps = 0
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = cat.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Series Tracker Card (Counting in live time)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_drill_tracker_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedCategory.accentColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Details Header
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedCategory.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = selectedCategory.icon,
                                    contentDescription = null,
                                    tint = selectedCategory.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = selectedCategory.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = selectedCategory.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Focus Cue / Tip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = selectedCategory.accentColor.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = selectedCategory.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedCategory.focusCue,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Series Objective & Timer Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Target reps adjuster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Objetivo:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            listOf(20, 30, 50).forEach { count ->
                                val isSelected = targetReps == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) selectedCategory.accentColor else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { targetReps = count }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Timer Badge & Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { isTimerRunning = !isTimerRunning }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isTimerRunning) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                val mins = elapsedSeconds / 60
                                val secs = elapsedSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Big Progress and Accuracy Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Repeticiones", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalCompletedReps",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " / $targetReps",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Precisión en la Serie", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = "${currentAccuracy.toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentAccuracy >= 75f) Color(0xFF00E676) else if (currentAccuracy >= 50f) Color(0xFFFFB300) else Color(0xFFFF5252)
                            )
                        }
                    }

                    // Progress Bar
                    val progress = (totalCompletedReps.toFloat() / targetReps.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = selectedCategory.accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Big Interactive Action Buttons (+1 Acierto / +1 Fallo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Success Button
                        Button(
                            onClick = {
                                if (!isTimerRunning && elapsedSeconds == 0) isTimerRunning = true
                                successfulReps += 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_drill_success"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Text("+1 Acierto", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Miss / Fault Button
                        Button(
                            onClick = {
                                if (!isTimerRunning && elapsedSeconds == 0) isTimerRunning = true
                                failedReps += 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_drill_fail"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                Text("+1 Fallo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Undo / Reset bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (successfulReps > 0 || failedReps > 0) {
                                TextButton(
                                    onClick = {
                                        if (successfulReps > 0) successfulReps -= 1
                                        else if (failedReps > 0) failedReps -= 1
                                    }
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Deshacer", fontSize = 12.sp)
                                }
                            }

                            TextButton(
                                onClick = {
                                    successfulReps = 0
                                    failedReps = 0
                                    elapsedSeconds = 0
                                    isTimerRunning = false
                                }
                            ) {
                                Text("Reiniciar Serie", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        // Difficulty picker
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Iniciación", "Intermedio", "Competición").forEach { diff ->
                                val isSel = difficultyLevel == diff
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { difficultyLevel = diff }
                                ) {
                                    Text(
                                        text = diff,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Save Series Button
                    Button(
                        onClick = {
                            val durationMins = (elapsedSeconds / 60).coerceAtLeast(1)
                            val finalTarget = if (totalCompletedReps > 0) totalCompletedReps else targetReps
                            val newSession = PadelDrillSession(
                                drillType = selectedCategory.name,
                                targetReps = finalTarget,
                                successfulReps = successfulReps,
                                durationMinutes = durationMins,
                                difficulty = difficultyLevel,
                                notes = technicalNotes.ifBlank { "Serie completada con éxito." }
                            )
                            coroutineScope.launch {
                                viewModel.saveDrillSession(newSession)
                                successfulReps = 0
                                failedReps = 0
                                elapsedSeconds = 0
                                isTimerRunning = false
                                technicalNotes = ""
                            }
                        },
                        enabled = totalCompletedReps > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_save_drill_session"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Text("Guardar Serie de Entrenamiento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 5. History of Logged Drills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial de Series Guardadas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${drills.size} registradas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (drills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Aún no tienes series de entrenamiento guardadas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Pulsa '+1 Acierto' o '+1 Fallo' arriba para registrar tu primera serie técnica en la pista.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(drills) { drill ->
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsTennis,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = drill.drillType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${drill.successfulReps} de ${drill.targetReps} aciertos (${drill.accuracyPercent.toInt()}%)",
                                    fontSize = 12.sp,
                                    color = if (drill.accuracyPercent >= 70f) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${dateFormat.format(Date(drill.timestamp))} • ${drill.durationMinutes} min • ${drill.difficulty}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteDrillSession(drill.id)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
