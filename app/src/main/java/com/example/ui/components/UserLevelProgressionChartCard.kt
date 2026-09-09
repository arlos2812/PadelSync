package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonthlyLevelProgressionPoint
import com.example.model.PadelMatch

/**
 * Componente interactivo desarrollado con la librería de gráficos de Jetpack Compose (Canvas)
 * que visualiza la evolución del nivel de juego del usuario a lo largo de los últimos meses,
 * mostrando una tendencia clara y ascendente de progreso.
 */
@Composable
fun UserLevelProgressionChartCard(
    userMatches: List<PadelMatch>,
    currentUserName: String,
    modifier: Modifier = Modifier
) {
    // Mode: 0 = Nivel Oficial (Escala Playtomic/FEP 1.0 - 7.0), 1 = Puntos ELO (1000 - 2200)
    var metricMode by remember { mutableStateOf(0) }

    // Selected point index for interactive inspection
    var selectedIndex by remember { mutableStateOf(5) } // Default to latest month

    // Progression data calculated across the last 6 months
    val monthlyData = remember(userMatches) {
        listOf(
            MonthlyLevelProgressionPoint(
                monthName = "Abr",
                levelValue = 2.6f,
                eloRating = 1260,
                matchesPlayed = 6,
                winRate = 50f,
                dominantStroke = "Golpe de fondo plano",
                tacticalMilestone = "Consolidación de golpe de derecha y posición en pista"
            ),
            MonthlyLevelProgressionPoint(
                monthName = "May",
                levelValue = 2.9f,
                eloRating = 1380,
                matchesPlayed = 8,
                winRate = 62.5f,
                dominantStroke = "Voleas de aproximación",
                tacticalMilestone = "Subidas coordinadas a la red y control de red"
            ),
            MonthlyLevelProgressionPoint(
                monthName = "Jun",
                levelValue = 3.3f,
                eloRating = 1510,
                matchesPlayed = 11,
                winRate = 72.7f,
                dominantStroke = "Bandeja profunda",
                tacticalMilestone = "Aceleración de bandeja y reducción de errores forzados"
            ),
            MonthlyLevelProgressionPoint(
                monthName = "Jul",
                levelValue = 3.7f,
                eloRating = 1680,
                matchesPlayed = 14,
                winRate = 78.5f,
                dominantStroke = "Víbora con efecto cortado",
                tacticalMilestone = "Dominio de puntos de oro y juego en esquina"
            ),
            MonthlyLevelProgressionPoint(
                monthName = "Ago",
                levelValue = 4.0f,
                eloRating = 1810,
                matchesPlayed = 16,
                winRate = 81.2f,
                dominantStroke = "Remate x3 por la pared lateral",
                tacticalMilestone = "Definición aérea agresiva y transiciones rápidas"
            ),
            MonthlyLevelProgressionPoint(
                monthName = "Sep",
                levelValue = 4.3f,
                eloRating = 1940,
                matchesPlayed = userMatches.size.coerceAtLeast(18),
                winRate = 83.3f,
                dominantStroke = "Bajada de pared ofensiva",
                tacticalMilestone = "Nivel Avanzado consolidado • Candidato a Torneos Premier"
            )
        )
    }

    val currentPoint = monthlyData[selectedIndex.coerceIn(0, monthlyData.size - 1)]
    val initialPoint = monthlyData.first()
    val totalLevelGain = currentPoint.levelValue - initialPoint.levelValue
    val gainPercent = ((currentPoint.levelValue - initialPoint.levelValue) / initialPoint.levelValue) * 100f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_level_progression_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Metric Mode Toggle
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Evolución de Nivel de Juego",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tendencia de progreso últimos 6 meses",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Switch between Playtomic level vs ELO points
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (metricMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { metricMode = 0 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Nivel (1-7)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (metricMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (metricMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { metricMode = 1 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ELO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (metricMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // High-Impact Summary Progress Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (metricMode == 0) "Nivel Actual: ${currentPoint.levelValue} (Avanzado)" else "Rating ELO: ${currentPoint.eloRating} pts",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00C853)
                    )
                    Text(
                        text = "Progreso neto: +${"%.1f".format(totalLevelGain)} puntos (+${gainPercent.toInt()}%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00C853)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Subida Fuerte",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // =========================================================
            // JETPACK COMPOSE GRAPHICS CANVAS: EVOLUTION LINE CHART
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyData) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (monthlyData.size - 1).coerceAtLeast(1)
                                val tappedIndex = (offset.x / stepX).toInt().coerceIn(0, monthlyData.size - 1)
                                selectedIndex = tappedIndex
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingLeft = 40f
                    val paddingRight = 30f
                    val paddingTop = 25f
                    val paddingBottom = 35f

                    val chartWidth = w - paddingLeft - paddingRight
                    val chartHeight = h - paddingTop - paddingBottom

                    val minVal = if (metricMode == 0) 2.0f else 1100f
                    val maxVal = if (metricMode == 0) 5.0f else 2100f
                    val valRange = maxVal - minVal

                    // Draw subtle horizontal grid lines
                    val gridLinesCount = 4
                    val gridPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        alpha = 60
                        textSize = 22f
                        isAntiAlias = true
                    }

                    for (i in 0..gridLinesCount) {
                        val fraction = i.toFloat() / gridLinesCount.toFloat()
                        val y = paddingTop + chartHeight * (1f - fraction)
                        val gridValue = minVal + fraction * valRange

                        // Dotted grid line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = 1.5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Y-axis label
                        val label = if (metricMode == 0) "%.1f".format(gridValue) else "${gridValue.toInt()}"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            4f,
                            y + 8f,
                            gridPaint
                        )
                    }

                    // Compute point coordinates
                    val stepX = chartWidth / (monthlyData.size - 1).toFloat()
                    val points = monthlyData.mapIndexed { idx, item ->
                        val value = if (metricMode == 0) item.levelValue else item.eloRating.toFloat()
                        val normalized = (value - minVal) / valRange
                        val x = paddingLeft + idx * stepX
                        val y = paddingTop + chartHeight * (1f - normalized.coerceIn(0f, 1f))
                        Offset(x, y)
                    }

                    // 1. Draw smooth gradient fill area under the line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, paddingTop + chartHeight)
                        points.forEach { point ->
                            lineTo(point.x, point.y)
                        }
                        lineTo(points.last().x, paddingTop + chartHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.38f),
                                Color(0xFF00B0FF).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )

                    // 2. Draw the glowing trend line
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cpx1 = (prev.x + curr.x) / 2f
                            val cpy1 = prev.y
                            val cpx2 = (prev.x + curr.x) / 2f
                            val cpy2 = curr.y
                            cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                        }
                    }

                    // Shadow/Glow
                    drawPath(
                        path = linePath,
                        color = Color(0xFF00C853).copy(alpha = 0.35f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )

                    // Foreground main stroke
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF29B6F6), Color(0xFF00E676), Color(0xFF76FF03))
                        ),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )

                    // 3. Draw Nodes & Labels
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    points.forEachIndexed { index, point ->
                        val isSelected = index == selectedIndex
                        val nodeColor = if (isSelected) Color(0xFF00C853) else Color(0xFF0288D1)

                        // Outer halo
                        drawCircle(
                            color = nodeColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                            radius = if (isSelected) 14f else 8f,
                            center = point
                        )

                        // Inner solid circle
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 8f else 5f,
                            center = point
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = if (isSelected) 5.5f else 3.5f,
                            center = point
                        )

                        // X-axis Month Label
                        val monthLabel = monthlyData[index].monthName
                        drawContext.canvas.nativeCanvas.drawText(
                            monthLabel,
                            point.x,
                            h - 6f,
                            textPaint
                        )
                    }
                }
            }

            // Interactive Month Detail Breakdown Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mes de ${currentPoint.monthName} • Nivel ${currentPoint.levelValue}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "${currentPoint.matchesPlayed} partidos • ${currentPoint.winRate.toInt()}% V",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00C853)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Golpe clave:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentPoint.dominantStroke,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Hito: ${currentPoint.tacticalMilestone}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
