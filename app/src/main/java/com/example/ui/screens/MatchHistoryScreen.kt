package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadelMatch
import com.example.ui.components.MatchScoreConfirmationDialog
import com.example.ui.components.PlayerPerformanceDashboardCard
import com.example.ui.components.UserLevelProgressionChartCard
import com.example.viewmodel.PadelViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    viewModel: PadelViewModel,
    onNavigateToMatchSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val allMatches by viewModel.allMatches.collectAsState()
    val loggedInName by viewModel.loggedInName.collectAsState()

    // Filter States
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("Todas") }
    var selectedCourtFilter by remember { mutableStateOf("Todas") }
    var selectedOpponentLevelFilter by remember { mutableStateOf("Todos") }
    var selectedResultFilter by remember { mutableStateOf("Todos") }
    var selectedTournamentFilter by remember { mutableStateOf("Todos") }
    var selectedFormatFilter by remember { mutableStateOf("Todos") }
    var sortDescending by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showManualRegisterDialog by remember { mutableStateOf(false) }
    var historyViewMode by remember { mutableStateOf(0) } // 0 = Partidos, 1 = Evolución de Nivel, 2 = Panel Rendimiento
    var selectedMatchForScoreConfirmation by remember { mutableStateOf<PadelMatch?>(null) }

    val dateOptions = listOf("Todas", "Hoy", "Últimos 7 días", "Últimos 30 días", "Últimos 3 meses", "Meses Anteriores")
    val courtOptions = listOf("Todas", "Cristal Panorámica", "Muro Tradicional", "Azul Premier", "Cubierta / Indoor", "Exterior")
    val opponentLevelOptions = listOf("Todos", "Iniciación", "Intermedio", "Avanzado", "Pro")
    val resultOptions = listOf("Todos", "Victorias", "Derrotas")
    val tournamentOptions = listOf("Todos", "Torneos Oficiales", "Liga", "Amistosos / Retos")

    if (selectedMatchForScoreConfirmation != null) {
        MatchScoreConfirmationDialog(
            match = selectedMatchForScoreConfirmation!!,
            viewModel = viewModel,
            onDismiss = { selectedMatchForScoreConfirmation = null }
        )
    }

    if (showFilterSheet) {
        MatchAdvancedFiltersDialog(
            selectedDateFilter = selectedDateFilter,
            selectedCourtFilter = selectedCourtFilter,
            selectedOpponentLevelFilter = selectedOpponentLevelFilter,
            selectedResultFilter = selectedResultFilter,
            dateOptions = dateOptions,
            courtOptions = courtOptions,
            opponentLevelOptions = opponentLevelOptions,
            resultOptions = resultOptions,
            onSelectDate = { selectedDateFilter = it },
            onSelectCourt = { selectedCourtFilter = it },
            onSelectOpponentLevel = { selectedOpponentLevelFilter = it },
            onSelectResult = { selectedResultFilter = it },
            onResetFilters = {
                selectedDateFilter = "Todas"
                selectedCourtFilter = "Todas"
                selectedOpponentLevelFilter = "Todos"
                selectedResultFilter = "Todos"
                searchQuery = ""
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showManualRegisterDialog) {
        ManualPadelMatchDialog(
            viewModel = viewModel,
            onDismiss = { showManualRegisterDialog = false }
        )
    }

    // Filter Logic
    val filteredMatches = remember(
        allMatches,
        searchQuery,
        selectedDateFilter,
        selectedCourtFilter,
        selectedOpponentLevelFilter,
        selectedResultFilter,
        selectedTournamentFilter,
        selectedFormatFilter,
        sortDescending
    ) {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L
        val sevenDays = oneDay * 7
        val thirtyDays = oneDay * 30
        val ninetyDays = oneDay * 90

        allMatches.filter { match ->
            // Search by opponent or player name
            val matchPlayers = "${match.player1A} ${match.player1B} ${match.player2A} ${match.player2B}"
            val matchesSearch = if (searchQuery.isBlank()) true else {
                matchPlayers.contains(searchQuery, ignoreCase = true) ||
                match.tournamentName.contains(searchQuery, ignoreCase = true) ||
                match.courtType.contains(searchQuery, ignoreCase = true)
            }

            // Date filtering (Rango de fechas)
            val diffMs = now - match.timestamp
            val matchesDate = when (selectedDateFilter) {
                "Hoy" -> diffMs < oneDay
                "Últimos 7 días" -> diffMs < sevenDays
                "Últimos 30 días" -> diffMs < thirtyDays
                "Últimos 3 meses" -> diffMs < ninetyDays
                "Meses Anteriores" -> diffMs >= thirtyDays
                else -> true
            }

            // Court filtering (Tipo de pista)
            val matchesCourt = when (selectedCourtFilter) {
                "Cristal Panorámica" -> match.courtType.contains("Cristal", ignoreCase = true) || match.courtType.contains("Panorámica", ignoreCase = true)
                "Muro Tradicional" -> match.courtType.contains("Muro", ignoreCase = true) || match.courtType.contains("Tradicional", ignoreCase = true)
                "Azul Premier" -> match.courtType.contains("Premier", ignoreCase = true) || match.courtType.contains("Azul", ignoreCase = true)
                "Cubierta / Indoor" -> match.courtType.contains("Cubierta", ignoreCase = true) || match.courtType.contains("Indoor", ignoreCase = true)
                "Exterior" -> match.courtType.contains("Exterior", ignoreCase = true) || match.courtType.contains("Outdoor", ignoreCase = true)
                else -> true
            }

            // Opponent level filtering (Nivel del rival)
            val oppLevel = viewModel.getOpponentLevelForMatch(match)
            val matchesOpponentLevel = when (selectedOpponentLevelFilter) {
                "Iniciación" -> oppLevel.equals("Iniciación", ignoreCase = true)
                "Intermedio" -> oppLevel.equals("Intermedio", ignoreCase = true)
                "Avanzado" -> oppLevel.equals("Avanzado", ignoreCase = true)
                "Pro" -> oppLevel.equals("Pro", ignoreCase = true) || oppLevel.equals("Profesional", ignoreCase = true)
                else -> true
            }

            // Result filtering: Winner Team 1 vs Team 2
            val isUserTeam1 = match.player1A.equals(loggedInName, ignoreCase = true) || match.player1B.equals(loggedInName, ignoreCase = true)
            val userWon = if (isUserTeam1) match.winnerTeam == 1 else match.winnerTeam == 2

            val matchesResult = when (selectedResultFilter) {
                "Victorias" -> userWon
                "Derrotas" -> !userWon && match.winnerTeam != 0
                else -> true
            }

            // Tournament filtering
            val matchesTournament = when (selectedTournamentFilter) {
                "Torneos Oficiales" -> match.matchResultType == "Torneo" || match.tournamentName.contains("Torneo", ignoreCase = true) || match.tournamentName.contains("Premier", ignoreCase = true)
                "Liga" -> match.matchResultType == "Liga" || match.tournamentName.contains("Liga", ignoreCase = true)
                "Amistosos / Retos" -> match.matchResultType == "Amistoso" || match.matchResultType == "Reto ELO" || match.isFriendlyChallenge
                else -> true
            }

            // Format filtering
            val matchesFormat = when (selectedFormatFilter) {
                "3 Sets" -> match.matchFormat.contains("3 Sets", ignoreCase = true)
                "6 Sets" -> match.matchFormat.contains("6 Sets", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesDate && matchesCourt && matchesOpponentLevel && matchesResult && matchesTournament && matchesFormat
        }.sortedBy { if (sortDescending) -it.timestamp else it.timestamp }
    }

    val totalMatchesCount = filteredMatches.size
    val totalWins = remember(filteredMatches, loggedInName) {
        filteredMatches.count { match ->
            val isUserTeam1 = match.player1A.equals(loggedInName, ignoreCase = true) || match.player1B.equals(loggedInName, ignoreCase = true)
            if (isUserTeam1) match.winnerTeam == 1 else match.winnerTeam == 2
        }
    }
    val totalLosses = remember(filteredMatches, loggedInName) {
        filteredMatches.count { match ->
            val isUserTeam1 = match.player1A.equals(loggedInName, ignoreCase = true) || match.player1B.equals(loggedInName, ignoreCase = true)
            val userWon = if (isUserTeam1) match.winnerTeam == 1 else match.winnerTeam == 2
            !userWon && match.winnerTeam != 0
        }
    }
    val winPercentage = if (totalMatchesCount > 0) (totalWins.toFloat() / totalMatchesCount * 100).toInt() else 0
    val lossPercentage = if (totalMatchesCount > 0) 100 - winPercentage else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Primary View Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple(0, "Partidos", Icons.Default.SportsTennis),
                Triple(1, "Evolución Nivel", Icons.Default.ShowChart),
                Triple(2, "Rendimiento", Icons.Default.Leaderboard)
            ).forEach { (mode, title, icon) ->
                val isSelected = historyViewMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { historyViewMode = mode }
                        .padding(vertical = 8.dp)
                        .testTag("tab_history_mode_$mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (historyViewMode == 1) {
            // Compose Chart showing user level progression over recent months
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                UserLevelProgressionChartCard(userMatches = allMatches, currentUserName = loggedInName)
            }
        } else if (historyViewMode == 2) {
            // Dashboard with player performance statistics (win rate, strokes, avg points)
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                PlayerPerformanceDashboardCard(userMatches = allMatches, playerName = loggedInName)
            }
        } else {
            // Search Bar with Filter Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("match_history_search_input"),
                placeholder = { Text("Buscar rival, pareja o torneo...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar texto", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Sort toggle button
            IconButton(
                onClick = { sortDescending = !sortDescending },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("btn_sort_matches")
            ) {
                Icon(
                    imageVector = if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "Cambiar orden",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Filter Horizontal Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Result chips
            resultOptions.forEach { opt ->
                val isSelected = selectedResultFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedResultFilter = opt },
                    label = {
                        Text(
                            text = when (opt) {
                                "Victorias" -> "🏆 Victorias"
                                "Derrotas" -> "❌ Derrotas"
                                else -> "Todos los Resultados"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Date chips
            dateOptions.forEach { opt ->
                val isSelected = selectedDateFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDateFilter = opt },
                    label = {
                        Text(
                            text = "📅 $opt",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Court chips (Tipo de pista)
            courtOptions.forEach { opt ->
                val isSelected = selectedCourtFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCourtFilter = opt },
                    label = {
                        Text(
                            text = when (opt) {
                                "Cristal Panorámica" -> "🎾 Cristal Panorámica"
                                "Muro Tradicional" -> "🧱 Muro Tradicional"
                                "Azul Premier" -> "🔷 Azul Premier"
                                "Cubierta / Indoor" -> "🏠 Indoor"
                                "Exterior" -> "☀️ Exterior"
                                else -> "Pista: Todas"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }

            // Opponent Level chips (Nivel del rival)
            opponentLevelOptions.forEach { opt ->
                val isSelected = selectedOpponentLevelFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedOpponentLevelFilter = opt },
                    label = {
                        Text(
                            text = if (opt == "Todos") "Rival: Todos los Niveles" else "⚡ Rival $opt",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Tournament chips
            tournamentOptions.forEach { opt ->
                val isSelected = selectedTournamentFilter == opt
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTournamentFilter = opt },
                    label = {
                        Text(
                            text = "🏟️ $opt",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Visual Win/Loss Percentage & Performance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("win_loss_visual_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Historial & Porcentajes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                com.example.util.PadelPdfReportExporter.generateAndSharePdfReport(context, loggedInName, allMatches)
                            },
                            modifier = Modifier.size(28.dp).testTag("btn_export_pdf_history")
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar Informe PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.syncStatsToFirebase() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Sincronizar con Firebase",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = { showManualRegisterDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Win & Loss percentage labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🏆 $winPercentage% Victorias ($totalWins)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "❌ $lossPercentage% Derrotas ($totalLosses)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                // Visual Horizontal Progress Bar (green vs red)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE57373))
                ) {
                    val winFraction = if (totalMatchesCount > 0) totalWins.toFloat() / totalMatchesCount.toFloat() else 0.5f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(winFraction)
                            .background(Color(0xFF4CAF50))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalMatchesCount partidos en Room",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    val hasActiveFilters = searchQuery.isNotBlank() || selectedDateFilter != "Todas" || selectedResultFilter != "Todos"
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedDateFilter = "Todas"
                                selectedResultFilter = "Todos"
                                selectedTournamentFilter = "Todos"
                                selectedFormatFilter = "Todos"
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Restablecer filtros", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Matches List
        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No se encontraron partidos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Prueba cambiando los filtros de fecha, resultado o búsqueda de rivales.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedDateFilter = "Todas"
                            selectedResultFilter = "Todos"
                            selectedTournamentFilter = "Todos"
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver Todos los Partidos")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredMatches, key = { it.id }) { match ->
                    MatchHistoryCard(
                        match = match,
                        currentUserName = loggedInName,
                        onConfirmScore = { selectedMatchForScoreConfirmation = match },
                        onShare = {
                            val isUserTeam1 = match.player1A.equals(loggedInName, ignoreCase = true) || match.player1B.equals(loggedInName, ignoreCase = true)
                            val userWon = if (isUserTeam1) match.winnerTeam == 1 else match.winnerTeam == 2
                            val resultText = if (userWon) "🏆 ¡Victoria!" else "Derrota"

                            val shareText = """
                                🎾 ACTA DE PARTIDO - PADEL TRACKER
                                🏟️ Torneo: ${match.tournamentName}
                                📅 Fecha: ${formatDate(match.timestamp)}
                                📊 Resultado: $resultText
                                👥 Pareja 1: ${match.player1A} & ${match.player1B}
                                👥 Pareja 2: ${match.player2A} & ${match.player2B}
                                🔢 Sets: ${formatSetsDisplay(match.setsTeam1, match.setsTeam2)}
                                ⏱️ Duración: ${match.durationMinutes} min • Pista: ${match.courtType}
                                Registrado con la app oficial de Padel Tracker.
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Acta de Partido"))
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
fun MatchHistoryCard(
    match: PadelMatch,
    currentUserName: String,
    onConfirmScore: () -> Unit = {},
    onShare: () -> Unit
) {
    val isUserTeam1 = match.player1A.equals(currentUserName, ignoreCase = true) || match.player1B.equals(currentUserName, ignoreCase = true)
    val userWon = if (isUserTeam1) match.winnerTeam == 1 else match.winnerTeam == 2
    val resultColor = if (userWon) Color(0xFF4CAF50) else Color(0xFFE53935)
    val resultLabel = if (userWon) "VICTORIA" else "DERROTA"

    val sets1 = match.setsTeam1.split(",").filter { it.isNotBlank() }
    val sets2 = match.setsTeam2.split(",").filter { it.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_history_item_${match.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, resultColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Tournament, Date, Result Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.tournamentName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "📅 ${formatDate(match.timestamp)} • ⏱️ ${match.durationMinutes} min",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(resultColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = resultLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = resultColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Teams & Score Table
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Team 1 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (match.winnerTeam == 1) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${match.player1A} / ${match.player1B}",
                            fontSize = 13.sp,
                            fontWeight = if (match.winnerTeam == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUserTeam1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sets scores
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sets1.forEachIndexed { idx, s1 ->
                            val isSetWon = (s1.toIntOrNull() ?: 0) > (sets2.getOrNull(idx)?.toIntOrNull() ?: 0)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSetWon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s1,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSetWon) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSetWon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Team 2 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (match.winnerTeam == 2) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${match.player2A} / ${match.player2B}",
                            fontSize = 13.sp,
                            fontWeight = if (match.winnerTeam == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isUserTeam1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sets scores
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sets2.forEachIndexed { idx, s2 ->
                            val isSetWon = (s2.toIntOrNull() ?: 0) > (sets1.getOrNull(idx)?.toIntOrNull() ?: 0)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSetWon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s2,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSetWon) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSetWon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val confirmedList = remember(match.confirmedByPlayers) {
                        match.confirmedByPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }
                    val isConfirmed = match.isScoreConfirmed || confirmedList.size >= 4

                    // Score confirmation badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isConfirmed) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isConfirmed) Color(0xFF2E7D32).copy(alpha = 0.35f) else Color(0xFFFF9800).copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onConfirmScore() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isConfirmed) Icons.Default.Verified else Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = if (isConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isConfirmed) "Marcador Homologado" else "Pendiente (${confirmedList.size}/4)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (match.scheduledStartTime.isNotBlank()) "${match.scheduledStartTime}-${match.scheduledEndTime}" else "${match.durationMinutes} min",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Button to confirm score or edit hours
                    TextButton(
                        onClick = onConfirmScore,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Acta / Horas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir Acta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES"))
    return sdf.format(Date(timestamp))
}

private fun formatSetsDisplay(setsT1: String, setsT2: String): String {
    val s1 = setsT1.split(",").filter { it.isNotBlank() }
    val s2 = setsT2.split(",").filter { it.isNotBlank() }
    val pairs = mutableListOf<String>()
    for (i in 0 until maxOf(s1.size, s2.size)) {
        val score1 = s1.getOrElse(i) { "0" }
        val score2 = s2.getOrElse(i) { "0" }
        pairs.add("$score1-$score2")
    }
    return pairs.joinToString(", ")
}

@Composable
fun ManualPadelMatchDialog(
    viewModel: PadelViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var p1A by remember { mutableStateOf(viewModel.loggedInName.value) }
    var p1B by remember { mutableStateOf("Compañero") }
    var p2A by remember { mutableStateOf("Rival 1") }
    var p2B by remember { mutableStateOf("Rival 2") }
    var sets1 by remember { mutableStateOf("6,6") }
    var sets2 by remember { mutableStateOf("4,3") }
    var tournament by remember { mutableStateOf("Torneo de Fin de Semana") }
    var courtType by remember { mutableStateOf("Cristal Panorámico") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SportsTennis,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Partido (Reglas FIP)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Introduce los jugadores y los tanteos por set separados por coma (ej. 6,6 vs 4,3 o 7,6 vs 5,7). Se verificará que cumpla las reglas oficiales de pádel.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = p1A,
                    onValueChange = { p1A = it },
                    label = { Text("Pareja 1 - Jugador A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = p1B,
                    onValueChange = { p1B = it },
                    label = { Text("Pareja 1 - Jugador B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = p2A,
                    onValueChange = { p2A = it },
                    label = { Text("Pareja 2 - Jugador A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = p2B,
                    onValueChange = { p2B = it },
                    label = { Text("Pareja 2 - Jugador B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sets1,
                        onValueChange = { sets1 = it },
                        label = { Text("Sets Pareja 1 (ej: 6,7)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sets2,
                        onValueChange = { sets2 = it },
                        label = { Text("Sets Pareja 2 (ej: 4,5)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tournament,
                    onValueChange = { tournament = it },
                    label = { Text("Nombre del Torneo o Amistoso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = viewModel.registerManualMatch(
                        p1A = p1A,
                        p1B = p1B,
                        p2A = p2A,
                        p2B = p2B,
                        sets1 = sets1,
                        sets2 = sets2,
                        tournament = tournament,
                        courtType = courtType
                    )
                    if (result.first) {
                        Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                        onDismiss()
                    } else {
                        errorMessage = result.second
                    }
                }
            ) {
                Text("Validar y Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchAdvancedFiltersDialog(
    selectedDateFilter: String,
    selectedCourtFilter: String,
    selectedOpponentLevelFilter: String,
    selectedResultFilter: String,
    dateOptions: List<String>,
    courtOptions: List<String>,
    opponentLevelOptions: List<String>,
    resultOptions: List<String>,
    onSelectDate: (String) -> Unit,
    onSelectCourt: (String) -> Unit,
    onSelectOpponentLevel: (String) -> Unit,
    onSelectResult: (String) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Filtros Avanzados de Partidos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Rango de Fechas
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📅 Rango de Fechas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dateOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedDateFilter == opt,
                                onClick = { onSelectDate(opt) },
                                label = { Text(opt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 2. Tipo de Pista
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🏟️ Tipo de Pista",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        courtOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedCourtFilter == opt,
                                onClick = { onSelectCourt(opt) },
                                label = { Text(opt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 3. Nivel del Rival
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⚡ Nivel del Rival",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        opponentLevelOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedOpponentLevelFilter == opt,
                                onClick = { onSelectOpponentLevel(opt) },
                                label = { Text(opt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 4. Resultado del Partido
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🏆 Resultado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        resultOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedResultFilter == opt,
                                onClick = { onSelectResult(opt) },
                                label = { Text(opt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Aplicar Filtros")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onResetFilters()
                    onDismiss()
                }
            ) {
                Text("Restablecer", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

