package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadelMatch
import com.example.ui.components.BluetoothWearableSyncCard
import com.example.ui.components.MatchScoreConfirmationDialog
import com.example.ui.screens.MatchHistoryScreen
import com.example.ui.screens.PadelTrainingScreen
import com.example.viewmodel.PadelViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun MatchScreen(viewModel: PadelViewModel) {
    val scrollState = rememberScrollState()

    // Sub-tab: 0 = "Marcador en Directo", 1 = "Historial", 2 = "Lo Mejor Premier", 3 = "Entrenar"
    var matchSubTab by remember { mutableStateOf(0) }

    // Auto-Sync and Network status
    val isOnline by viewModel.isNetworkOnline.collectAsState()
    val isSyncing by viewModel.isAutoSyncRunning.collectAsState()
    val unsyncedMatches by viewModel.unsyncedMatches.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (viewModel.showVictoryDialog && viewModel.lastFinishedMatch != null) {
        VictoryDialog(
            match = viewModel.lastFinishedMatch!!,
            viewModel = viewModel,
            onDismiss = { viewModel.showVictoryDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Switcher Header: Marcador vs Historial vs Lo Mejor Premier vs Entrenar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Marcador
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (matchSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { matchSubTab = 0 }
                    .padding(vertical = 9.dp)
                    .testTag("tab_match_tracker"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = null,
                        tint = if (matchSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Marcador",
                        fontSize = 11.sp,
                        fontWeight = if (matchSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (matchSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Tab 1: Historial
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (matchSubTab == 1) MaterialTheme.colorScheme.secondary else Color.Transparent)
                    .clickable { matchSubTab = 1 }
                    .padding(vertical = 9.dp)
                    .testTag("tab_match_history"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = if (matchSubTab == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Historial",
                        fontSize = 11.sp,
                        fontWeight = if (matchSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (matchSubTab == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Tab 2: Lo Mejor Premier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (matchSubTab == 2) Color(0xFFFF9800) else Color.Transparent)
                    .clickable { matchSubTab = 2 }
                    .padding(vertical = 9.dp)
                    .testTag("tab_premier_highlights"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = null,
                        tint = if (matchSubTab == 2) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Lo Mejor",
                        fontSize = 11.sp,
                        fontWeight = if (matchSubTab == 2) FontWeight.Bold else FontWeight.Medium,
                        color = if (matchSubTab == 2) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Tab 3: Entrenamiento
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (matchSubTab == 3) Color(0xFF00C853) else Color.Transparent)
                    .clickable { matchSubTab = 3 }
                    .padding(vertical = 9.dp)
                    .testTag("tab_training_mode"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (matchSubTab == 3) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Entrenar",
                        fontSize = 11.sp,
                        fontWeight = if (matchSubTab == 3) FontWeight.Bold else FontWeight.Medium,
                        color = if (matchSubTab == 3) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Room <-> Firestore Auto-Sync Status Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isOnline) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFFFE082).copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSyncing) Color(0xFF29B6F6)
                                else if (isOnline) Color(0xFF00E676)
                                else Color(0xFFFF9800)
                            )
                    )
                    Text(
                        text = if (isSyncing) "Sincronizando Room con Firestore..."
                        else if (!isOnline) "Modo Offline (Room local activo - sync automático al conectar)"
                        else if (unsyncedMatches.isNotEmpty()) "${unsyncedMatches.size} partido(s) pendiente(s) de sincronizar"
                        else "Room y Firestore sincronizados automáticamente",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                if (isOnline && unsyncedMatches.isNotEmpty()) {
                    Text(
                        text = "Sincronizar Ya",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                viewModel.syncPendingMatchesToFirestore()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (matchSubTab) {
            1 -> {
                MatchHistoryScreen(
                    viewModel = viewModel,
                    onNavigateToMatchSetup = { matchSubTab = 0 }
                )
            }
            2 -> {
                PremierHighlightsScreen(viewModel = viewModel)
            }
            3 -> {
                PadelTrainingScreen(viewModel = viewModel)
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!viewModel.isMatchInProgress) {
                        NewMatchSetup(viewModel)
                    } else {
                        ActiveMatchScorer(viewModel)
                        Spacer(modifier = Modifier.height(8.dp))
                        WearOSWatchSimulator(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun VictoryDialog(
    match: PadelMatch,
    viewModel: PadelViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val winningPair = if (match.winnerTeam == 1) "${match.player1A} & ${match.player1B}" else "${match.player2A} & ${match.player2B}"
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        MatchScoreConfirmationDialog(
            match = match,
            viewModel = viewModel,
            onDismiss = { showConfirmationDialog = false }
        )
    }

    val confirmedList = remember(match.confirmedByPlayers) {
        match.confirmedByPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val isFullyConfirmed = match.isScoreConfirmed || confirmedList.size >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showConfirmationDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFullyConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("btn_open_score_confirm_dialog")
                ) {
                    Icon(
                        if (isFullyConfirmed) Icons.Default.Verified else Icons.Default.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFullyConfirmed) "Ver Acta (4/4)" else "Confirmar Marcador", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val shareText = """
                            🏆 ¡PARTIDAZO DE PÁDEL FINALIZADO!
                            Ganadores: $winningPair
                            Resultado: ${match.setsTeam1} vs ${match.setsTeam2}
                            Horario: ${match.scheduledStartTime} - ${match.scheduledEndTime} (${match.durationMinutes} min)
                            Pista: ${match.courtType} (${match.matchFormat})
                            Estado: ${if (isFullyConfirmed) "Acta Homologada 100%" else "Pendiente de confirmación"}
                            Registrado en vivo con Padel Tracker Pro
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir resultado de pádel")
                        context.startActivity(shareIntent)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("¡Victoria en Pádel!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🏆 Campeones: $winningPair",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Marcador por sets: ${match.setsTeam1}  vs  ${match.setsTeam2}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "📅 ${match.scheduledDate.ifBlank { "Hoy" }} • ⏰ ${match.scheduledStartTime} - ${match.scheduledEndTime} (${match.durationMinutes} min)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Score Confirmation Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFullyConfirmed) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isFullyConfirmed) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFFF9800).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullyConfirmed) Icons.Default.Verified else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isFullyConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFullyConfirmed) "Acta Homologada (4/4 jugadores han confirmado)"
                            else "Marcador ratificado por ${confirmedList.size}/4 jugadores",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800)
                        )
                    }
                }

                Text(
                    text = "Puedes ajustar el horario jugado (inicio y fin) y confirmar el marcador entre todos los participantes.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun NewMatchSetup(viewModel: PadelViewModel) {
    var p1A by remember { mutableStateOf(viewModel.matchP1A) }
    var p1B by remember { mutableStateOf(viewModel.matchP1B) }
    var p2A by remember { mutableStateOf(viewModel.matchP2A) }
    var p2B by remember { mutableStateOf(viewModel.matchP2B) }

    val currentFormat by viewModel.matchFormat.collectAsState()
    val voiceEnabled by viewModel.voiceAnnouncer.collectAsState()
    val soundFxEnabled by viewModel.soundFx.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_setup_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nuevo Partido de Pádel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start
                )
                Icon(
                    Icons.Default.SportsTennis,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Bluetooth Wearable Status in Setup
            BluetoothWearableSyncCard(viewModel = viewModel)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Scheduling Section: Match Day & Hours
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Fecha y Horario del Partido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Antes de jugar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Day Selection Chips
                Text("Elige el día:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Hoy", "Mañana", "Sábado", "Domingo").forEach { dayOption ->
                        val isSelected = viewModel.scheduledMatchDate.startsWith(dayOption) || (dayOption == "Hoy" && viewModel.scheduledMatchDate.contains("Hoy"))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.scheduledMatchDate = when (dayOption) {
                                    "Hoy" -> "Hoy (08/09/2026)"
                                    "Mañana" -> "Mañana (09/09/2026)"
                                    "Sábado" -> "Sábado (12/09/2026)"
                                    else -> "Domingo (13/09/2026)"
                                }
                            },
                            label = { Text(dayOption, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Hours selection
                Text("Horario programado (de qué hora a qué hora):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.scheduledStartTime,
                        onValueChange = { viewModel.scheduledStartTime = it },
                        label = { Text("Hora Inicio") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("input_scheduled_start_time"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.scheduledEndTime,
                        onValueChange = { viewModel.scheduledEndTime = it },
                        label = { Text("Hora Fin") },
                        leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("input_scheduled_end_time"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Quick hours presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(Pair("18:00", "19:30"), Pair("18:30", "20:00"), Pair("20:00", "21:30")).forEach { (start, end) ->
                        SuggestionChip(
                            onClick = {
                                viewModel.scheduledStartTime = start
                                viewModel.scheduledEndTime = end
                            },
                            label = { Text("$start-$end", fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Player Invitation status bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Convocatoria de Jugadores",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Invita a tus compañeros y rivales para que acepten el partido.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Button(
                    onClick = {
                        viewModel.player1BInvitationStatus = "Aceptado"
                        viewModel.player2AInvitationStatus = "Aceptado"
                        viewModel.player2BInvitationStatus = "Aceptado"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aceptar Todos", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Team 1 Configuration
            Text(
                text = "Pareja 1 (Lado A)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = p1A,
                        onValueChange = {
                            p1A = it
                            viewModel.matchP1A = it
                        },
                        label = { Text("Jugador 1 (Tú)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_1a_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "✓ Creador (Aceptado)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = p1B,
                        onValueChange = {
                            p1B = it
                            viewModel.matchP1B = it
                        },
                        label = { Text("Jugador 2 (Compañero)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_1b_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAccepted = viewModel.player1BInvitationStatus == "Aceptado"
                        Surface(
                            color = if (isAccepted) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isAccepted) "✓ Aceptado" else "⏳ ${viewModel.player1BInvitationStatus}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccepted) Color(0xFF2E7D32) else Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                if (viewModel.player1BInvitationStatus == "Aceptado") {
                                    viewModel.player1BInvitationStatus = "Invitación enviada"
                                } else {
                                    viewModel.sendMatchInvitation("p1B", p1B)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(if (isAccepted) "Reenviar" else "Invitar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Team 2 Configuration
            Text(
                text = "Pareja 2 (Lado B - Rivales)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = p2A,
                        onValueChange = {
                            p2A = it
                            viewModel.matchP2A = it
                        },
                        label = { Text("Jugador 3 (Rival 1)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_2a_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAccepted = viewModel.player2AInvitationStatus == "Aceptado"
                        Surface(
                            color = if (isAccepted) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isAccepted) "✓ Aceptado" else "⏳ ${viewModel.player2AInvitationStatus}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccepted) Color(0xFF2E7D32) else Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                if (viewModel.player2AInvitationStatus == "Aceptado") {
                                    viewModel.player2AInvitationStatus = "Invitación enviada"
                                } else {
                                    viewModel.sendMatchInvitation("p2A", p2A)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(if (isAccepted) "Reenviar" else "Invitar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = p2B,
                        onValueChange = {
                            p2B = it
                            viewModel.matchP2B = it
                        },
                        label = { Text("Jugador 4 (Rival 2)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_2b_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAccepted = viewModel.player2BInvitationStatus == "Aceptado"
                        Surface(
                            color = if (isAccepted) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isAccepted) "✓ Aceptado" else "⏳ ${viewModel.player2BInvitationStatus}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccepted) Color(0xFF2E7D32) else Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                if (viewModel.player2BInvitationStatus == "Aceptado") {
                                    viewModel.player2BInvitationStatus = "Invitación enviada"
                                } else {
                                    viewModel.sendMatchInvitation("p2B", p2B)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(if (isAccepted) "Reenviar" else "Invitar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Match Format Selection
            Text(
                text = "Formato de Encuentro (Hasta 6 Sets y Tie-Break)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("3 Sets", "6 Sets", "Pro Set (9)", "Set Único").forEach { format ->
                    val selected = currentFormat == format
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setMatchFormat(format) },
                        label = { Text(format, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Audio Referee & Score Call Switches
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Árbitro de Voz en Español",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Canta los puntos (\"15 iguales\", \"Ventaja\") por altavoz o auriculares Bluetooth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { viewModel.toggleVoiceAnnouncer() }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (viewModel.isGoldenPoint) "Punto de Oro (Padel WPT/Premier)" else "Ventaja Tradicional",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (viewModel.isGoldenPoint) "En 40-40, el siguiente punto define el juego." else "Requiere ganar por margen de 2 en deuce.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = viewModel.isGoldenPoint,
                        onCheckedChange = { viewModel.isGoldenPoint = it },
                        modifier = Modifier.testTag("golden_point_switch")
                    )
                }
            }

            Button(
                onClick = { viewModel.startNewMatch(p1A, p1B, p2A, p2B) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_match_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INICIAR PARTIDO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PadelCourtVisualizer(
    serverName: String,
    isRightSide: Boolean,
    isTieBreak: Boolean
) {
    val courtColor = Color(0xFF0D47A1)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PISTA EN VIVO • VISTA TÁCTICA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (isRightSide) "SAQUE: LADO DERECHO" else "SAQUE: LADO IZQUIERDO",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRightSide) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
            )
        }

        // 2D Padel Court representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(courtColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pad = 12f

                // Outer boundary lines (walls)
                drawRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(pad, pad),
                    size = Size(w - 2 * pad, h - 2 * pad),
                    style = Stroke(width = 2.5f)
                )

                // Center Net
                val netX = w / 2f
                drawLine(
                    color = Color.White,
                    start = Offset(netX, pad - 2),
                    end = Offset(netX, h - pad + 2),
                    strokeWidth = 3f
                )

                // Service Lines (Left and Right sides of the net)
                val serviceLineDistFromNet = (w / 2f - pad) * 0.65f
                val leftServiceLineX = netX - serviceLineDistFromNet
                val rightServiceLineX = netX + serviceLineDistFromNet

                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(leftServiceLineX, pad),
                    end = Offset(leftServiceLineX, h - pad),
                    strokeWidth = 2f
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(rightServiceLineX, pad),
                    end = Offset(rightServiceLineX, h - pad),
                    strokeWidth = 2f
                )

                // Center T lines (divides Deuce and Ad service boxes)
                val midY = h / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(leftServiceLineX, midY),
                    end = Offset(netX, midY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(netX, midY),
                    end = Offset(rightServiceLineX, midY),
                    strokeWidth = 2f
                )

                // Highlight Active Server Position and Target diagonal receiver
                val serverX = if (isRightSide) w * 0.18f else w * 0.18f
                val serverY = if (isRightSide) h * 0.72f else h * 0.28f

                val targetX = if (isRightSide) w * 0.62f else w * 0.62f
                val targetY = if (isRightSide) h * 0.28f else h * 0.72f

                // Active server indicator
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = pulseAlpha),
                    radius = 9f,
                    center = Offset(serverX, serverY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(serverX, serverY)
                )

                // Target receiver box highlight
                drawCircle(
                    color = Color(0xFF69F0AE).copy(alpha = pulseAlpha),
                    radius = 8f,
                    center = Offset(targetX, targetY)
                )
            }

            // Overlay label
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Saca $serverName 🎾 → Cruzado al receptor",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ActiveMatchScorer(viewModel: PadelViewModel) {
    var showEarlyFinishConfirm by remember { mutableStateOf(false) }
    val matchFormat by viewModel.matchFormat.collectAsState()

    if (showEarlyFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showEarlyFinishConfirm = false },
            title = { Text("¿Terminar Partido?", fontWeight = FontWeight.Bold) },
            text = { Text("Se guardará el resultado alcanzado hasta este momento en tu historial.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEarlyFinishConfirm = false
                        viewModel.finishMatch(1)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Finalizar y Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEarlyFinishConfirm = false }) {
                    Text("Continuar jugando")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_match_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Real-time Bluetooth Wearable & Heart Rate Sync Module
            BluetoothWearableSyncCard(viewModel = viewModel)

            // Live Header with Stopwatch, Announcer button, and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Dot + Stopwatch Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Red.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.getFormattedMatchTime(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.togglePauseTimer() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pausar/Reanudar cronómetro",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Voice Announcer Button (Can tap to replay score call aloud)
                FilledTonalIconButton(
                    onClick = { viewModel.speakScoreCall() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Cantar marcador con voz de árbitro",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Format & Surface Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.isGoldenPoint) "Punto de Oro" else "Con Ventaja",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tie-Break Alert Banner (Official padel rules at 6-6)
            AnimatedVisibility(visible = viewModel.isTieBreak) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF8F00), Color(0xFFFFB300))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¡TIE-BREAK EN JUEGO! A 7 PUNTOS (DIF. 2)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            // Teams & Score Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team names list
                Column(
                    modifier = Modifier.weight(1.4f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Team 1 Row
                    Column {
                        Text(
                            text = "${viewModel.matchP1A} / ${viewModel.matchP1B}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (viewModel.serverPlayerIdx == 0 || viewModel.serverPlayerIdx == 2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saca: ${if (viewModel.serverPlayerIdx == 0) viewModel.matchP1A else viewModel.matchP1B}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Team 2 Row
                    Column {
                        Text(
                            text = "${viewModel.matchP2A} / ${viewModel.matchP2B}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (viewModel.serverPlayerIdx == 1 || viewModel.serverPlayerIdx == 3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saca: ${if (viewModel.serverPlayerIdx == 1) viewModel.matchP2A else viewModel.matchP2B}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sets & Points Grid
                Row(
                    modifier = Modifier.weight(1.3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set Games tracker (Dynamic: 3 or 6 Sets)
                    val totalSetsToShow = if (matchFormat.contains("6 Sets")) 6 else if (matchFormat.contains("Set Único") || matchFormat.contains("Pro Set")) 1 else 3
                    val setBoxSize = if (totalSetsToShow == 6) 22.dp else 26.dp
                    val setFontSize = if (totalSetsToShow == 6) 11.sp else 13.sp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until totalSetsToShow) {
                                val games = viewModel.team1SetScores.value[i]
                                val isActive = viewModel.currentSetIndex == i
                                Box(
                                    modifier = Modifier
                                        .size(setBoxSize)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = games.toString(),
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = setFontSize,
                                        color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until totalSetsToShow) {
                                val games = viewModel.team2SetScores.value[i]
                                val isActive = viewModel.currentSetIndex == i
                                Box(
                                    modifier = Modifier
                                        .size(setBoxSize)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = games.toString(),
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = setFontSize,
                                        color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Big Current Points column (Handles Tie-Break or normal points)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp, 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.isTieBreak) "${viewModel.p1TieBreakPoints}" else viewModel.getPointsString(viewModel.p1PointsIdx),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp, 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.isTieBreak) "${viewModel.p2TieBreakPoints}" else viewModel.getPointsString(viewModel.p2PointsIdx),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }

            // 2D Mini Padel Court Visualizer showing serve direction & court side
            PadelCourtVisualizer(
                serverName = viewModel.getActiveServerName(),
                isRightSide = viewModel.isServerOnRightSide(),
                isTieBreak = viewModel.isTieBreak
            )

            // Interactive Point Scoring Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.addPointToTeam(1) },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("score_t1_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PUNTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Pareja 1", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Button(
                    onClick = { viewModel.addPointToTeam(2) },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("score_t2_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PUNTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Pareja 2", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Botones para sumar JUEGOS directamente a cada pareja (según reglas oficiales del pádel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addGameToTeam(1) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("add_game_t1_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+1 Juego Pareja 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.addGameToTeam(2) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("add_game_t2_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+1 Juego Pareja 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Padel Rules Status Banner (Lado de saque, cambio de lado de pista y sets)
            val currentGamesP1 = viewModel.team1SetScores.value.getOrElse(viewModel.currentSetIndex) { 0 }
            val currentGamesP2 = viewModel.team2SetScores.value.getOrElse(viewModel.currentSetIndex) { 0 }
            val totalGamesInSet = currentGamesP1 + currentGamesP2
            val isCourtSideChange = totalGamesInSet > 0 && totalGamesInSet % 2 == 1

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("padel_rules_indicator"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lado de saque: ${viewModel.currentServeSide}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isCourtSideChange) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Cambio de pista",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    } else if (viewModel.isTieBreak) {
                        Text(
                            text = "Tie-Break FIP (saque cada 2 pts)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Game Controls: Undo, Set indicator, Early finish button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.undoLastScore() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .testTag("undo_score_button")
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Deshacer punto",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Set Actual: ${viewModel.currentSetIndex + 1} (${matchFormat})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Button(
                    onClick = { showEarlyFinishConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("finish_match_early_button")
                ) {
                    Text("Terminar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}


enum class WatchBrand(val label: String, val appName: String, val desc: String) {
    APPLE("Apple Watch", "watchOS", "Anotador rápido táctil"),
    WEAR_OS("Wear OS", "Google/Samsung", "Sincronización nativa"),
    GARMIN("Garmin", "ConnectIQ", "Bajo consumo"),
    XIAOMI("Xiaomi Watch", "HyperOS", "Ligero y deportivo"),
    HUAWEI("Huawei Watch", "HarmonyOS", "Alta precisión"),
    GENERIC("Cualquier Marca", "BLE Universal", "Polar, Amazfit, etc.")
}

@Composable
fun WatchBrandPill(
    brand: WatchBrand,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    when (brand) {
                        WatchBrand.APPLE -> Color(0xFFFF5722).copy(alpha = 0.15f)
                        WatchBrand.WEAR_OS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        WatchBrand.GARMIN -> Color(0xFFC0FF00).copy(alpha = 0.15f)
                        WatchBrand.XIAOMI -> Color(0xFFFF6D00).copy(alpha = 0.15f)
                        WatchBrand.HUAWEI -> Color(0xFFE53935).copy(alpha = 0.12f)
                        WatchBrand.GENERIC -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                    }
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    when (brand) {
                        WatchBrand.APPLE -> Color(0xFFFF5722)
                        WatchBrand.WEAR_OS -> MaterialTheme.colorScheme.primary
                        WatchBrand.GARMIN -> Color(0xFFC0FF00)
                        WatchBrand.XIAOMI -> Color(0xFFFF6D00)
                        WatchBrand.HUAWEI -> Color(0xFFE53935)
                        WatchBrand.GENERIC -> Color(0xFF00E5FF)
                    }
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = brand.label,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                text = brand.appName,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun WearOSWatchSimulator(viewModel: PadelViewModel) {
    val isSynced by viewModel.isWatchSynced.collectAsState()
    var isVibrating by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var activeVibrationTeam by remember { mutableStateOf(0) }
    var selectedWatchBrand by remember { mutableStateOf(WatchBrand.APPLE) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Beautiful dynamic glow based on touch scoring haptic simulation
    val borderGlowColor by animateColorAsState(
        targetValue = if (isVibrating) {
            if (activeVibrationTeam == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        } else {
            if (isSynced) {
                when (selectedWatchBrand) {
                    WatchBrand.APPLE -> Color(0xFFFF5722) // Apple Orange
                    WatchBrand.WEAR_OS -> MaterialTheme.colorScheme.primary
                    WatchBrand.GARMIN -> Color(0xFFC0FF00) // Garmin Neon
                    WatchBrand.XIAOMI -> Color(0xFFFF6D00) // Xiaomi Sport Orange
                    WatchBrand.HUAWEI -> Color(0xFFE53935) // Huawei Red
                    WatchBrand.GENERIC -> Color(0xFF00E5FF) // Universal Cyan
                }
            } else {
                Color.Gray
            }
        },
        animationSpec = tween(durationMillis = 300),
        label = "glowColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isVibrating) 8.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "borderWidth"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wear_os_simulator_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Watch connection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronización con Smartwatch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { viewModel.toggleWatchSync() },
                    modifier = Modifier.testTag("toggle_watch_sync_btn")
                ) {
                    Text(
                        text = if (isSynced) "Conectado" else "Desconectado",
                        color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "¡Anota puntos directo en tu muñeca sin sacar el móvil! Selecciona tu reloj inteligente para activar la sincronización universal:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Dynamic Watch Brand Selection Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Apple, Wear OS, Garmin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(WatchBrand.APPLE, WatchBrand.WEAR_OS, WatchBrand.GARMIN).forEach { brand ->
                        Box(modifier = Modifier.weight(1f)) {
                            WatchBrandPill(
                                brand = brand,
                                isSelected = selectedWatchBrand == brand,
                                onClick = { selectedWatchBrand = brand }
                            )
                        }
                    }
                }
                // Row 2: Xiaomi, Huawei, Generic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(WatchBrand.XIAOMI, WatchBrand.HUAWEI, WatchBrand.GENERIC).forEach { brand ->
                        Box(modifier = Modifier.weight(1f)) {
                            WatchBrandPill(
                                brand = brand,
                                isSelected = selectedWatchBrand == brand,
                                onClick = { selectedWatchBrand = brand }
                            )
                        }
                    }
                }
            }

            // Universal Watch Interactive Simulation Case
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Determine watch physical casing shape
                val watchShape = if (selectedWatchBrand == WatchBrand.APPLE) RoundedCornerShape(32.dp) else CircleShape

                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .shadow(10.dp, watchShape)
                        .clip(watchShape)
                        .background(
                            when (selectedWatchBrand) {
                                WatchBrand.APPLE -> Color(0xFF000000)
                                WatchBrand.WEAR_OS -> Color(0xFF0F1416)
                                WatchBrand.GARMIN -> Color(0xFF14171A)
                                WatchBrand.XIAOMI -> Color(0xFF0B0D0F)
                                WatchBrand.HUAWEI -> Color(0xFF07080A)
                                WatchBrand.GENERIC -> Color(0xFF101920)
                            }
                        )
                        .border(
                            BorderStroke(borderWidth, borderGlowColor),
                            watchShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isSynced) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PULSA CONECTAR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        // Screen Split scoring areas custom layout per model
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Watch Top status (Non-clickable overlay)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.45f)
                                    .padding(top = if (selectedWatchBrand == WatchBrand.APPLE) 12.dp else 10.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsTennis,
                                        contentDescription = null,
                                        tint = when (selectedWatchBrand) {
                                            WatchBrand.APPLE -> Color(0xFFFF5722)
                                            WatchBrand.WEAR_OS -> MaterialTheme.colorScheme.primary
                                            WatchBrand.GARMIN -> Color(0xFFC0FF00)
                                            WatchBrand.XIAOMI -> Color(0xFFFF6D00)
                                            WatchBrand.HUAWEI -> Color(0xFFE53935)
                                            WatchBrand.GENERIC -> Color(0xFF00E5FF)
                                        },
                                        modifier = Modifier.size(10.dp)
                                    )
                                    val currentSetNum = viewModel.currentSetIndex + 1
                                    val gamesP1 = viewModel.team1SetScores.value[viewModel.currentSetIndex]
                                    val gamesP2 = viewModel.team2SetScores.value[viewModel.currentSetIndex]
                                    Text(
                                        text = "SET $currentSetNum ($gamesP1-$gamesP2) • ${selectedWatchBrand.appName}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.9f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Tactile Split Score Area (Left / Right)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f)
                            ) {
                                // Left Side: Team 1 (+1 Point)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .clickable {
                                            viewModel.addPointToTeam(1)
                                            activeVibrationTeam = 1
                                            isVibrating = true
                                            coroutineScope.launch {
                                                delay(350)
                                                isVibrating = false
                                            }
                                            if (soundEnabled) {
                                                Toast.makeText(context, "📳 ${selectedWatchBrand.label}: +1 Pareja 1", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .testTag("watch_btn_t1"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "PAREJA 1",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (viewModel.isTieBreak) "${viewModel.p1TieBreakPoints}" else viewModel.getPointsString(viewModel.p1PointsIdx),
                                            fontSize = if (selectedWatchBrand == WatchBrand.APPLE) 34.sp else 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                // Center separator line
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(Color.White.copy(alpha = 0.12f))
                                )

                                // Right Side: Team 2 (+1 Point)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .clickable {
                                            viewModel.addPointToTeam(2)
                                            activeVibrationTeam = 2
                                            isVibrating = true
                                            coroutineScope.launch {
                                                delay(350)
                                                isVibrating = false
                                            }
                                            if (soundEnabled) {
                                                Toast.makeText(context, "📳 ${selectedWatchBrand.label}: +1 Pareja 2", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .testTag("watch_btn_t2"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "PAREJA 2",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (viewModel.isTieBreak) "${viewModel.p2TieBreakPoints}" else viewModel.getPointsString(viewModel.p2PointsIdx),
                                            fontSize = if (selectedWatchBrand == WatchBrand.APPLE) 34.sp else 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            // Watch Bottom Server Bar (Tap to toggle server directly from watch)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.45f)
                                    .clickable {
                                        viewModel.switchServerFromWatch()
                                        Toast.makeText(context, "🎾 Saque cambiado desde reloj", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(bottom = if (selectedWatchBrand == WatchBrand.APPLE) 12.dp else 10.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val serverName = viewModel.getActiveServerName().substringBefore(" ")
                                Text(
                                    text = "🎾 Saca: $serverName ⟳",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Smartwatch physical side hardware buttons simulating natural watch controls
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    // Digital Crown Button (Undo action)
                    Card(
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .clickable {
                                viewModel.undoLastScore()
                                Toast.makeText(context, "↩️ Sincro: Punto deshecho", Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Deshacer punto en reloj",
                                modifier = Modifier.size(14.dp)
                              )
                            Text("Atrás", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Second Button (Haptic / Sound mode)
                    Card(
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .clickable {
                                soundEnabled = !soundEnabled
                                val msg = if (soundEnabled) "Vibración reloj activada 📳" else "Vibración silenciada"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (soundEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Configuración háptica",
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Hápico", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Smartwatch Quick Modification Controls (Points, Games, Server, Undo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Modificar tanteo desde el Reloj (${selectedWatchBrand.label}):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.awardGameFromWatch(1)
                            Toast.makeText(context, "🎾 +1 Juego Pareja 1 anotado desde reloj", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("watch_award_game_t1"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+1 JUEGO", fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text("Pareja 1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.awardGameFromWatch(2)
                            Toast.makeText(context, "🎾 +1 Juego Pareja 2 anotado desde reloj", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("watch_award_game_t2"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+1 JUEGO", fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Text("Pareja 2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.switchServerFromWatch()
                            Toast.makeText(context, "🎾 Saque cambiado desde el reloj", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("watch_switch_server"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Saque", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dynamic set summary inside companion panel
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val setG1 = viewModel.team1SetScores.value.take(viewModel.currentSetIndex + 1).joinToString("-")
                val setG2 = viewModel.team2SetScores.value.take(viewModel.currentSetIndex + 1).joinToString("-")
                
                Text(
                    text = "Estado global en ${selectedWatchBrand.label}:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "($setG1) vs ($setG2)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
