package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import com.example.model.PadelMatch
import com.example.ui.components.PadelCourtHeatmapCard
import com.example.ui.components.HeadToHeadComparisonCard
import com.example.util.PadelPdfReportExporter
import com.example.viewmodel.PadelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class ChartTypeMode {
    PIE,
    BAR
}

data class StrokeStat(
    val name: String,
    val percentage: Float, // 0.0 to 1.0
    val countPerMatch: Int,
    val color: Color,
    val description: String
)

data class WinnerStat(
    val strokeName: String,
    val effectiveness: Float, // 0.0 to 1.0
    val winnersCount: Int,
    val attemptsCount: Int,
    val tag: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: PadelViewModel) {
    val matches by viewModel.allMatches.collectAsState()
    val loggedInName by viewModel.loggedInName.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Period filter: 0 = Histórico, 1 = Últimos 10, 2 = Este Mes
    var selectedPeriod by remember { mutableStateOf(0) }

    // Toggle chart view for stroke frequency: Pie vs Bar
    var strokeChartMode by remember { mutableStateOf(ChartTypeMode.BAR) }

    // Toggle chart view for win rate: Pie vs Bar
    var winRateChartMode by remember { mutableStateOf(ChartTypeMode.PIE) }

    // Toggle chart view for winners effectiveness: Bar vs Pie
    var winnerChartMode by remember { mutableStateOf(ChartTypeMode.BAR) }

    // Compute user match statistics
    val userMatches = remember(matches, loggedInName) {
        matches.filter {
            it.player1A == loggedInName || it.player1B == loggedInName ||
            it.player2A == loggedInName || it.player2B == loggedInName ||
            it.player1A == "Yo (Tú)" || it.player1B == "Yo (Tú)" ||
            it.player2A == "Yo (Tú)" || it.player2B == "Yo (Tú)"
        }
    }

    val actualPlayed = userMatches.size
    val actualWon = userMatches.count { match ->
        val userIsTeam1 = match.player1A == loggedInName || match.player1B == loggedInName || match.player1A == "Yo (Tú)" || match.player1B == "Yo (Tú)"
        val userIsTeam2 = match.player2A == loggedInName || match.player2B == loggedInName || match.player2A == "Yo (Tú)" || match.player2B == "Yo (Tú)"
        (userIsTeam1 && match.winnerTeam == 1) || (userIsTeam2 && match.winnerTeam == 2)
    }

    // If user has zero recorded matches yet, provide realistic demo data with prominent preview tag
    val hasRealMatches = actualPlayed > 0
    val totalPlayed = if (hasRealMatches) actualPlayed else 16
    val totalWon = if (hasRealMatches) actualWon else 12
    val totalLost = totalPlayed - totalWon
    val winRate = (totalWon.toFloat() / totalPlayed.toFloat()) * 100f

    // Stroke frequency data distribution in Padel
    val strokeStats = remember {
        listOf(
            StrokeStat("Bandeja", 0.32f, 48, Color(0xFF2196F3), "Golpe de control y colocación"),
            StrokeStat("Volea de Red", 0.24f, 36, Color(0xFF00E676), "Presión constante ofensiva"),
            StrokeStat("Víbora", 0.16f, 24, Color(0xFFFF9800), "Corte lateral venenoso"),
            StrokeStat("Globo Defensivo", 0.14f, 21, Color(0xFF9C27B0), "Recuperación de red"),
            StrokeStat("Remate / Smash", 0.10f, 15, Color(0xFFE91E63), "Definición por potencia"),
            StrokeStat("Bajada de Pared", 0.04f, 6, Color(0xFF00BCD4), "Ataque rápido de rebote")
        )
    }

    // Winner points effectiveness data
    val winnerStats = remember {
        listOf(
            WinnerStat("Remate x3 / Potencia", 0.85f, 34, 40, "Sobresaliente", Color(0xFFE91E63)),
            WinnerStat("Volea a la Reja", 0.78f, 28, 36, "Alta", Color(0xFF00E676)),
            WinnerStat("Bajada Ofensiva", 0.72f, 18, 25, "Óptima", Color(0xFF00BCD4)),
            WinnerStat("Víbora al Rincón", 0.69f, 22, 32, "Buena", Color(0xFFFF9800)),
            WinnerStat("Saque Directo / Primer Servicio", 0.62f, 16, 26, "En Progreso", Color(0xFF2196F3))
        )
    }

    // Screen Sub-tab: 0 = Métricas y Rendimiento, 1 = Historial Filtrado de Partidos
    var statsSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab Switcher Header: Métricas vs Mapa de Pista vs Cara a Cara vs Historial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Rendimiento
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (statsSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { statsSubTab = 0 }
                    .padding(vertical = 8.dp)
                    .testTag("tab_stats_metrics"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Métricas",
                    fontSize = 11.sp,
                    fontWeight = if (statsSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (statsSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Tab 1: Mapa de Pista
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (statsSubTab == 1) Color(0xFF00B0FF) else Color.Transparent)
                    .clickable { statsSubTab = 1 }
                    .padding(vertical = 8.dp)
                    .testTag("tab_stats_heatmap"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pista",
                    fontSize = 11.sp,
                    fontWeight = if (statsSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (statsSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Tab 2: Cara a Cara
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (statsSubTab == 2) Color(0xFFFF9800) else Color.Transparent)
                    .clickable { statsSubTab = 2 }
                    .padding(vertical = 8.dp)
                    .testTag("tab_stats_head_to_head"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cara a Cara",
                    fontSize = 11.sp,
                    fontWeight = if (statsSubTab == 2) FontWeight.Bold else FontWeight.Medium,
                    color = if (statsSubTab == 2) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Tab 3: Historial con Filtros
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (statsSubTab == 3) MaterialTheme.colorScheme.secondary else Color.Transparent)
                    .clickable { statsSubTab = 3 }
                    .padding(vertical = 8.dp)
                    .testTag("tab_stats_history"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Historial",
                    fontSize = 11.sp,
                    fontWeight = if (statsSubTab == 3) FontWeight.Bold else FontWeight.Medium,
                    color = if (statsSubTab == 3) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (statsSubTab) {
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PadelCourtHeatmapCard(
                        userMatches = userMatches,
                        allMatches = matches,
                        loggedInName = loggedInName
                    )
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeadToHeadComparisonCard(
                        userMatches = userMatches,
                        allMatches = matches,
                        currentUserName = loggedInName
                    )
                }
            }
            3 -> {
                MatchHistoryScreen(viewModel = viewModel)
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
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estadísticas & Rendimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Análisis táctico de golpes y efectividad",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        PadelPdfReportExporter.generateAndSharePdfReport(context, loggedInName, matches)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_export_pdf_stats")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Exportar PDF",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Period Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Histórico Global", "Últimos 10", "Este Mes").forEachIndexed { index, periodTitle ->
                val isSelected = selectedPeriod == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedPeriod = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = periodTitle,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (!hasRealMatches) {
            // Informative Notice about Sample Baseline
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mostrando estadísticas de referencia para tu nivel. Los datos se actualizarán automáticamente con tus próximos partidos.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Summary Metric Cards: Matches, Wins, Losses, ELO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = "Partidos",
                value = "$totalPlayed",
                subtext = "Jugados",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Victorias",
                value = "$totalWon",
                subtext = "${winRate.toInt()}% efectividad",
                color = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Derrotas",
                value = "$totalLost",
                subtext = "A mejorar",
                color = Color(0xFFFF5252),
                modifier = Modifier.weight(1f)
            )
        }

        // ==========================================
        // SECCIÓN: TENDENCIA DE RENDIMIENTO (ÚLTIMOS 10 PARTIDOS) - GRÁFICO DE LÍNEAS
        // ==========================================
        MatchPerformanceTrendLineChartCard(
            userMatches = userMatches,
            allMatches = matches,
            loggedInName = loggedInName
        )

        // ==========================================
        // 1. SECCIÓN: PORCENTAJE DE VICTORIAS (PASTEL / BARRAS)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("win_rate_section_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with chart mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Porcentaje de Victorias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Chart Mode Toggle (Pastel vs Barras)
                    ChartModeToggle(
                        selectedMode = winRateChartMode,
                        onModeSelected = { winRateChartMode = it },
                        tag = "winrate"
                    )
                }

                if (winRateChartMode == ChartTypeMode.PIE) {
                    // Pie / Donut Chart for Win Rate
                    WinRatePieChart(
                        winRate = winRate,
                        totalWon = totalWon,
                        totalLost = totalLost,
                        totalPlayed = totalPlayed
                    )
                } else {
                    // Bar Chart for Win Rate
                    WinRateBarChart(
                        totalWon = totalWon,
                        totalLost = totalLost,
                        totalPlayed = totalPlayed
                    )
                }
            }
        }

        // ==========================================
        // 2. SECCIÓN: TIPO DE GOLPES MÁS FRECUENTES (BARRAS / PASTEL)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stroke_frequency_section_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with chart mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Golpes Más Frecuentes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Distribución táctica durante el juego",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Chart Mode Toggle (Barras vs Pastel)
                    ChartModeToggle(
                        selectedMode = strokeChartMode,
                        onModeSelected = { strokeChartMode = it },
                        tag = "stroke"
                    )
                }

                if (strokeChartMode == ChartTypeMode.BAR) {
                    // Bar Chart: Horizontal bars with distinct colors
                    StrokeFrequencyBarChart(strokes = strokeStats)
                } else {
                    // Pie Chart: Multi-sector donut with legend
                    StrokeFrequencyPieChart(strokes = strokeStats)
                }
            }
        }

        // ==========================================
        // 3. SECCIÓN: EFECTIVIDAD DE PUNTOS GANADORES (WINNERS EFFECTIVENESS)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("winners_effectiveness_section_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Efectividad de Puntos Ganadores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tasa de conversión en definición",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    ChartModeToggle(
                        selectedMode = winnerChartMode,
                        onModeSelected = { winnerChartMode = it },
                        tag = "winner"
                    )
                }

                // Global Winners vs Unforced Errors KPI Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Winners Directos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("118", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                        Text("68% Total", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Errores No Forzados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("56", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252))
                        Text("32% Total", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ratio W/E", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("+2.1x", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("Positivo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (winnerChartMode == ChartTypeMode.BAR) {
                    // Bar Chart: Effectiveness by defining shot
                    WinnersEffectivenessBarChart(winners = winnerStats)
                } else {
                    // Pie / Donut Chart: Winner vs Errors Ratio + Pie breakdown
                    WinnersRatioPieChart(winners = winnerStats)
                }
            }
        }

        // AI Coaching Tactical Advice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Consejo Táctico de IA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Tu Remate tiene una efectividad letal del 85%, pero tu Saque sólo convierte el 62% de puntos ganados. Trabaja en la profundidad y el efecto cortado de tu primer servicio para forzar más errores en el resto rival.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }

        // ==========================================
        // MAPA DE CALOR INTERACTIVO DE PUNTOS GANADORES EN PISTA
        // ==========================================
        PadelCourtHeatmapCard(
            userMatches = userMatches,
            allMatches = matches,
            loggedInName = loggedInName
        )

        // ==========================================
        // COMPARATIVA CARA A CARA (HEAD-TO-HEAD) ENTRE JUGADORES
        // ==========================================
        HeadToHeadComparisonCard(
            userMatches = userMatches,
            allMatches = matches,
            currentUserName = loggedInName
        )
    }
        }
    }
}
}

// -------------------------------------------------------------
// COMPONENTES DE GRÁFICOS (PASTEL Y BARRAS)
// -------------------------------------------------------------

/**
 * Toggle selector between Pie Chart and Bar Chart
 */
@Composable
fun ChartModeToggle(
    selectedMode: ChartTypeMode,
    onModeSelected: (ChartTypeMode) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(2.dp)
            .testTag("chart_mode_toggle_$tag"),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Bar Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedMode == ChartTypeMode.BAR) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onModeSelected(ChartTypeMode.BAR) }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Barras",
                    modifier = Modifier.size(13.dp),
                    tint = if (selectedMode == ChartTypeMode.BAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Barras",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMode == ChartTypeMode.BAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Pie Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedMode == ChartTypeMode.PIE) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onModeSelected(ChartTypeMode.PIE) }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = "Pastel",
                    modifier = Modifier.size(13.dp),
                    tint = if (selectedMode == ChartTypeMode.PIE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pastel",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMode == ChartTypeMode.PIE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 1A. Gráfico de Pastel para Porcentaje de Victorias (Win Rate Pie Chart)
 */
@Composable
fun WinRatePieChart(
    winRate: Float,
    totalWon: Int,
    totalLost: Int,
    totalPlayed: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winrate_pie_chart"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pie / Donut Canvas
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val winColor = Color(0xFF00E676)
            val lossColor = Color(0xFFFF5252)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 18.dp.toPx()
                val sweepWin = (winRate / 100f) * 360f
                val sweepLoss = 360f - sweepWin

                // Draw Wins arc
                drawArc(
                    color = winColor,
                    startAngle = -90f,
                    sweepAngle = sweepWin,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Draw Losses arc
                if (sweepLoss > 0f) {
                    drawArc(
                        color = lossColor,
                        startAngle = -90f + sweepWin,
                        sweepAngle = sweepLoss,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${winRate.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "VICTORIA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Legend & Breakdown details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wins Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Text("Victorias", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$totalWon partidos (${winRate.toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }

            // Losses Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF5252)))
                    Text("Derrotas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                val lossRate = 100 - winRate.toInt()
                Text(
                    text = "$totalLost partidos ($lossRate%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Total summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total disputados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("$totalPlayed partidos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 1B. Gráfico de Barras para Porcentaje de Victorias (Win Rate Bar Chart)
 */
@Composable
fun WinRateBarChart(
    totalWon: Int,
    totalLost: Int,
    totalPlayed: Int
) {
    val winRate = (totalWon.toFloat() / totalPlayed.toFloat())
    val lossRate = (totalLost.toFloat() / totalPlayed.toFloat())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winrate_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Wins Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Text("Victorias Conquistadas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("$totalWon victorias (${(winRate * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(winRate.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFF00E676))))
                )
            }
        }

        // Losses Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5252)))
                    Text("Derrotas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("$totalLost derrotas (${(lossRate * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(lossRate.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFD50000), Color(0xFFFF5252))))
                )
            }
        }
    }
}

/**
 * 2A. Gráfico de Barras para Tipo de Golpes Más Frecuentes (Stroke Frequency Bar Chart)
 */
@Composable
fun StrokeFrequencyBarChart(strokes: List<StrokeStat>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stroke_frequency_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        strokes.forEach { stroke ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stroke.color)
                        )
                        Text(
                            text = stroke.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${stroke.countPerMatch} golpes/partido",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${(stroke.percentage * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = stroke.color
                        )
                    }
                }

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    // Maximum percentage in dataset is 0.32, so we normalize to full width for visual impact
                    val normalizedProgress = (stroke.percentage / 0.35f).coerceIn(0.05f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(normalizedProgress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(stroke.color.copy(alpha = 0.7f), stroke.color)
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * 2B. Gráfico de Pastel para Tipo de Golpes Más Frecuentes (Stroke Frequency Pie Chart)
 */
@Composable
fun StrokeFrequencyPieChart(strokes: List<StrokeStat>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stroke_frequency_pie_chart"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-segment Pie Chart Canvas
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 28.dp.toPx()
                var startAngle = -90f

                strokes.forEach { stroke ->
                    val sweepAngle = stroke.percentage * 360f
                    drawArc(
                        color = stroke.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SportsTennis,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "100%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "DISTRIBUCIÓN",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Legend Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            strokes.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(item.color))
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(item.percentage * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = item.color
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 3A. Gráfico de Barras para Efectividad de Puntos Ganadores
 */
@Composable
fun WinnersEffectivenessBarChart(winners: List<WinnerStat>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winners_effectiveness_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        winners.forEach { stat ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stat.strokeName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(stat.color.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stat.tag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = stat.color
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${stat.winnersCount}/${stat.attemptsCount} ganados",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${(stat.effectiveness * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = stat.color
                        )
                    }
                }

                // Effectiveness Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(stat.effectiveness)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(stat.color.copy(alpha = 0.7f), stat.color)
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * 3B. Gráfico de Pastel / Donut para Efectividad de Puntos Ganadores
 */
@Composable
fun WinnersRatioPieChart(winners: List<WinnerStat>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winners_effectiveness_pie_chart"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Donut Ratio (Winners vs Errors)
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val winnerColor = Color(0xFF00E676)
            val errorColor = Color(0xFFFF5252)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 18.dp.toPx()
                val sweepWinner = 0.68f * 360f
                val sweepError = 360f - sweepWinner

                drawArc(
                    color = winnerColor,
                    startAngle = -90f,
                    sweepAngle = sweepWinner,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                drawArc(
                    color = errorColor,
                    startAngle = -90f + sweepWinner,
                    sweepAngle = sweepError,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "68%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676)
                )
                Text(
                    text = "WINNERS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Breakdown by defining stroke
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Efectividad por Golpe",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            winners.take(4).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.color))
                        Text(item.strokeName.substringBefore(" "), fontSize = 12.sp)
                    }
                    Text(
                        text = "${(item.effectiveness * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.color
                    )
                }
            }
        }
    }
}

/**
 * Summary Metric Tile Card
 */
@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
            Text(subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
        }
    }
}

/**
 * Data model for 10-match performance trend point
 */
data class MatchTrendPoint(
    val index: Int,
    val label: String,
    val isWin: Boolean,
    val winRatePercent: Float,
    val durationMinutes: Int,
    val scoreText: String,
    val rivalText: String,
    val dateText: String
)

/**
 * Interactive Line Chart showing performance trends (Win Rates & Game Duration)
 * for the user's last 10 matches.
 */
@Composable
fun MatchPerformanceTrendLineChartCard(
    userMatches: List<PadelMatch>,
    allMatches: List<PadelMatch>,
    loggedInName: String,
    modifier: Modifier = Modifier
) {
    val recentPoints = remember(userMatches, allMatches, loggedInName) {
        val targetList = if (userMatches.isNotEmpty()) userMatches else allMatches
        val sorted = targetList.sortedBy { it.timestamp }
        val sampleBaseline = listOf(
            MatchTrendPoint(1, "P1", true, 100f, 52, "6-4 6-2", "Carlos & Marc", "12 Ago"),
            MatchTrendPoint(2, "P2", true, 100f, 65, "7-5 6-4", "Lucas & David", "15 Ago"),
            MatchTrendPoint(3, "P3", false, 67f, 48, "4-6 5-7", "Alvaro & Jorge", "19 Ago"),
            MatchTrendPoint(4, "P4", true, 75f, 70, "6-3 4-6 6-4", "Sergio & Dani", "22 Ago"),
            MatchTrendPoint(5, "P5", true, 80f, 58, "6-2 6-4", "Nacho & Alex", "26 Ago"),
            MatchTrendPoint(6, "P6", false, 67f, 82, "6-7 7-6 5-7", "Pablo & Victor", "30 Ago"),
            MatchTrendPoint(7, "P7", true, 71f, 55, "6-3 6-2", "Adrian & Marcos", "02 Sep"),
            MatchTrendPoint(8, "P8", true, 75f, 60, "7-6 6-4", "Hugo & Guille", "04 Sep"),
            MatchTrendPoint(9, "P9", true, 78f, 50, "6-1 6-3", "Gonzalo & Ivan", "06 Sep"),
            MatchTrendPoint(10, "P10", true, 80f, 64, "6-4 3-6 7-5", "Rafa & Fernando", "Hoy")
        )

        if (sorted.isEmpty()) {
            sampleBaseline
        } else {
            val last10 = if (sorted.size >= 10) sorted.takeLast(10) else {
                val needed = 10 - sorted.size
                val prefix = sampleBaseline.take(needed)
                prefix + sorted.mapIndexed { idx, m ->
                    val isTeam1 = m.player1A == loggedInName || m.player1B == loggedInName || m.player1A == "Yo (Tú)" || m.player1B == "Yo (Tú)"
                    val isWin = (isTeam1 && m.winnerTeam == 1) || (!isTeam1 && m.winnerTeam == 2)
                    val dur = if (m.durationMinutes > 0) m.durationMinutes else 55 + (idx * 4) % 25
                    val s1 = m.setsTeam1.split(",").filter { it.isNotBlank() }
                    val s2 = m.setsTeam2.split(",").filter { it.isNotBlank() }
                    val score = if (s1.isNotEmpty() && s2.isNotEmpty()) {
                        s1.indices.joinToString(" ") { i -> "${s1.getOrElse(i) { "0" }}-${s2.getOrElse(i) { "0" }}" }
                    } else "6-4 6-3"
                    MatchTrendPoint(
                        index = prefix.size + idx + 1,
                        label = "P${prefix.size + idx + 1}",
                        isWin = isWin,
                        winRatePercent = 75f,
                        durationMinutes = dur,
                        scoreText = score,
                        rivalText = if (isTeam1) "${m.player2A} & ${m.player2B}" else "${m.player1A} & ${m.player1B}",
                        dateText = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(m.timestamp))
                    )
                }
            }

            var wins = 0
            last10.mapIndexed { index, m ->
                if (m is MatchTrendPoint) {
                    if (m.isWin) wins++
                    m.copy(
                        index = index + 1,
                        label = "P${index + 1}",
                        winRatePercent = (wins.toFloat() / (index + 1).toFloat()) * 100f
                    )
                } else {
                    val match = m as PadelMatch
                    val isTeam1 = match.player1A == loggedInName || match.player1B == loggedInName || match.player1A == "Yo (Tú)" || match.player1B == "Yo (Tú)"
                    val isWin = (isTeam1 && match.winnerTeam == 1) || (!isTeam1 && match.winnerTeam == 2)
                    if (isWin) wins++
                    val dur = if (match.durationMinutes > 0) match.durationMinutes else 55 + (index * 4) % 25
                    val s1 = match.setsTeam1.split(",").filter { it.isNotBlank() }
                    val s2 = match.setsTeam2.split(",").filter { it.isNotBlank() }
                    val score = if (s1.isNotEmpty() && s2.isNotEmpty()) {
                        s1.indices.joinToString(" ") { i -> "${s1.getOrElse(i) { "0" }}-${s2.getOrElse(i) { "0" }}" }
                    } else "6-4 6-3"
                    MatchTrendPoint(
                        index = index + 1,
                        label = "P${index + 1}",
                        isWin = isWin,
                        winRatePercent = (wins.toFloat() / (index + 1).toFloat()) * 100f,
                        durationMinutes = dur,
                        scoreText = score,
                        rivalText = if (isTeam1) "${match.player2A} & ${match.player2B}" else "${match.player1A} & ${match.player1B}",
                        dateText = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(match.timestamp))
                    )
                }
            }
        }
    }

    var selectedMetric by remember { mutableStateOf(0) } // 0 = % Victorias, 1 = Duración, 2 = Dual
    var selectedPointIndex by remember { mutableStateOf(recentPoints.size - 1) }
    val activePoint = recentPoints.getOrNull(selectedPointIndex) ?: recentPoints.last()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_performance_trend_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Tendencia de Rendimiento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Línea temporal de los últimos 10 partidos",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Trend badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "10 Partidos",
                        color = Color(0xFF00C853),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metric Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("% Victorias", "Duración (min)", "Vista Dual").forEachIndexed { index, title ->
                    val isSelected = selectedMetric == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedMetric = index }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Legend when in Dual mode
            if (selectedMetric == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("% Victorias", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00B0FF)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Duración (min)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Canvas Line Chart
            val isDark = MaterialTheme.colorScheme.surface.let {
                // Approximate luminance check
                val lum = 0.299 * it.red + 0.587 * it.green + 0.114 * it.blue
                lum < 0.5
            }
            val gridColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            val textPaintColor = if (isDark) android.graphics.Color.argb(140, 255, 255, 255) else android.graphics.Color.argb(140, 60, 60, 60)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("performance_line_chart_canvas")
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 38.dp.toPx()
                    val paddingRight = 18.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 26.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    val count = recentPoints.size
                    val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

                    val textPaint = Paint().apply {
                        color = textPaintColor
                        textSize = 24f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }

                    // Draw Horizontal Grid Lines & Y-Axis Labels
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val fraction = i.toFloat() / gridSteps.toFloat()
                        val y = paddingTop + chartHeight * (1f - fraction)
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        val label = when (selectedMetric) {
                            0, 2 -> "${(fraction * 100).toInt()}%"
                            else -> "${(30 + fraction * 80).toInt()}m"
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            4.dp.toPx(),
                            y + 8f,
                            textPaint
                        )
                    }

                    // Helper to calculate coordinate for a point
                    fun getCoord(index: Int, metric: Int): Offset {
                        val x = paddingLeft + index * stepX
                        val normalized = when (metric) {
                            0 -> {
                                val pt = recentPoints[index]
                                (pt.winRatePercent / 100f).coerceIn(0f, 1f)
                            }
                            1 -> {
                                val pt = recentPoints[index]
                                ((pt.durationMinutes - 30f) / 80f).coerceIn(0f, 1f)
                            }
                            else -> 0.5f
                        }
                        val y = paddingTop + chartHeight * (1f - normalized)
                        return Offset(x, y)
                    }

                    // Function to draw line & gradient area for a given metric
                    fun renderMetricPath(metric: Int, lineColor: Color) {
                        val path = Path()
                        val fillPath = Path()

                        val firstCoord = getCoord(0, metric)
                        path.moveTo(firstCoord.x, firstCoord.y)
                        fillPath.moveTo(firstCoord.x, paddingTop + chartHeight)
                        fillPath.lineTo(firstCoord.x, firstCoord.y)

                        for (i in 1 until count) {
                            val prev = getCoord(i - 1, metric)
                            val curr = getCoord(i, metric)
                            val cx = (prev.x + curr.x) / 2f
                            path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                            fillPath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }

                        val lastCoord = getCoord(count - 1, metric)
                        fillPath.lineTo(lastCoord.x, paddingTop + chartHeight)
                        fillPath.close()

                        // Draw Gradient Area
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Draw Stroke Line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw Point Anchors
                        for (i in 0 until count) {
                            val coord = getCoord(i, metric)
                            val isSelected = i == selectedPointIndex
                            val pt = recentPoints[i]

                            // Outer halo if selected
                            if (isSelected) {
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.25f),
                                    radius = 12.dp.toPx(),
                                    center = coord
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.5.dp.toPx(),
                                    center = coord
                                )
                            }

                            // Center dot
                            val centerColor = if (metric == 0 && !isSelected) {
                                if (pt.isWin) Color(0xFF00E676) else Color(0xFFFF5252)
                            } else {
                                lineColor
                            }

                            drawCircle(
                                color = centerColor,
                                radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = coord
                            )
                        }
                    }

                    // Render lines according to selectedMetric
                    when (selectedMetric) {
                        0 -> renderMetricPath(0, Color(0xFF00E676))
                        1 -> renderMetricPath(1, Color(0xFF00B0FF))
                        2 -> {
                            renderMetricPath(0, Color(0xFF00E676))
                            renderMetricPath(1, Color(0xFF00B0FF))
                        }
                    }

                    // Draw X-Axis Labels (P1 to P10)
                    for (i in 0 until count) {
                        val x = paddingLeft + i * stepX
                        val label = recentPoints[i].label
                        val isSelected = i == selectedPointIndex
                        val ptPaint = Paint().apply {
                            color = if (isSelected) android.graphics.Color.argb(240, 0, 230, 118) else textPaintColor
                            textSize = if (isSelected) 24f else 20f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                            typeface = if (isSelected) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            height - 4.dp.toPx(),
                            ptPaint
                        )
                    }
                }
            }

            // Quick Touch selector chips (P1..P10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                recentPoints.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedPointIndex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (pt.isWin) Color(0xFF00E676).copy(alpha = 0.12f)
                                else Color(0xFFFF5252).copy(alpha = 0.12f)
                            )
                            .clickable { selectedPointIndex = idx }
                            .testTag("trend_point_select_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else if (pt.isWin) Color(0xFF00C853)
                            else Color(0xFFFF5252)
                        )
                    }
                }
            }

            // Selected Match Details Inspection Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activePoint.isWin) Color(0xFF00E676).copy(alpha = 0.3f) else Color(0xFFFF5252).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Partido ${activePoint.label}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${activePoint.dateText}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activePoint.isWin) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (activePoint.isWin) "Victoria ✓" else "Derrota ✕",
                                color = if (activePoint.isWin) Color(0xFF00C853) else Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rivales", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(activePoint.rivalText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Marcador", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(activePoint.scoreText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Duración", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("${activePoint.durationMinutes} min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                        }
                        Column {
                            Text("Win Rate Acum.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("${activePoint.winRatePercent.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        }
                    }
                }
            }

            // Summary KPI Trend Metrics
            val avgDuration = recentPoints.map { it.durationMinutes }.average().toInt()
            val totalWins10 = recentPoints.count { it.isWin }
            val firstWr = recentPoints.first().winRatePercent
            val lastWr = recentPoints.last().winRatePercent
            val diffWr = (lastWr - firstWr).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Evolución 10P", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = if (diffWr >= 0) "+$diffWr%" else "$diffWr%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diffWr >= 0) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Duración Media", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "$avgDuration min",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00B0FF)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Balance 10P", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "${totalWins10}V - ${10 - totalWins10}D",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
