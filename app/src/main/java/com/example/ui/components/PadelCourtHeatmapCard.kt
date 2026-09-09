package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadelMatch

/**
 * Tactical zone definition for winning shots on the padel court
 */
data class CourtZoneHeat(
    val id: String,
    val name: String,
    val normalizedX: Float, // 0.0 to 1.0 on court width
    val normalizedY: Float, // 0.0 to 1.0 on court length
    val relativePercentage: Float, // e.g. 28%
    val winningPointsCount: Int,
    val dominantStroke: String, // "Bandeja", "Remate x3", "Volea", "Bajada"
    val tacticalTip: String,
    val heatLevel: Float // 0.0 to 1.0 for heat glow radius and intensity
)

/**
 * Interactive Padel Court Heatmap Component:
 * Renders a simplified, highly realistic top-down representation of a padel court,
 * with glowing heat clusters where winning shots land during recorded matches.
 */
@Composable
fun PadelCourtHeatmapCard(
    userMatches: List<PadelMatch>,
    allMatches: List<PadelMatch>,
    loggedInName: String,
    modifier: Modifier = Modifier
) {
    // Filter by stroke type
    var selectedStrokeFilter by remember { mutableStateOf("Todos") }
    // Filter by player perspective (0: Mis Puntos, 1: Rivales, 2: Ambos)
    var selectedPerspective by remember { mutableStateOf(0) }

    val totalMatchesCount = if (userMatches.isNotEmpty()) userMatches.size else (if (allMatches.isNotEmpty()) allMatches.size else 8)
    val baseMultiplier = (totalMatchesCount.coerceAtLeast(3) * 3.4f).toInt()

    // Calculated zone frequencies adapted to filter
    val courtZones = remember(selectedStrokeFilter, selectedPerspective, totalMatchesCount) {
        when (selectedStrokeFilter) {
            "Remates" -> listOf(
                CourtZoneHeat("x3_left", "Salida x3 (Lateral Izq.)", 0.08f, 0.42f, 34f, (baseMultiplier * 0.34f).toInt().coerceAtLeast(6), "Remate x3", "Efecto liftado potente con rebote alto para sacarla por la malla de 3m.", 0.95f),
                CourtZoneHeat("x3_right", "Salida x3 (Lateral Der.)", 0.92f, 0.42f, 26f, (baseMultiplier * 0.26f).toInt().coerceAtLeast(5), "Remate x3", "Remate cruzado hacia la malla lateral contraria.", 0.85f),
                CourtZoneHeat("fondo_t", "Remate a la T / Fondo", 0.50f, 0.88f, 22f, (baseMultiplier * 0.22f).toInt().coerceAtLeast(4), "Remate Plano", "Aceleración plana al centro para descolocar a la pareja rival.", 0.75f),
                CourtZoneHeat("esquina_der", "Fondo Cristal Derecho", 0.78f, 0.92f, 18f, (baseMultiplier * 0.18f).toInt().coerceAtLeast(3), "Remate x4", "Pelota que impacta en el cristal de fondo y sale disparada hacia arriba.", 0.65f)
            )
            "Bandejas" -> listOf(
                CourtZoneHeat("reja_izq", "Reja Cruzada Izquierda", 0.15f, 0.62f, 38f, (baseMultiplier * 0.38f).toInt().coerceAtLeast(8), "Víbora Cortada", "Impacto con mucho efecto lateral para provocar un bote impredecible.", 0.95f),
                CourtZoneHeat("reja_der", "Reja Cruzada Derecha", 0.85f, 0.62f, 30f, (baseMultiplier * 0.30f).toInt().coerceAtLeast(6), "Bandeja Profunda", "Bandeja lenta y colocada justo a la unión de reja y cristal.", 0.85f),
                CourtZoneHeat("doble_pared", "Doble Pared Esquina", 0.20f, 0.90f, 20f, (baseMultiplier * 0.20f).toInt().coerceAtLeast(4), "Bandeja al Rincón", "Bolas profundas que botan en doble pared obligando al giro del rival.", 0.70f),
                CourtZoneHeat("centro_fondo", "Pasillo Central (Nevera)", 0.50f, 0.75f, 12f, (baseMultiplier * 0.12f).toInt().coerceAtLeast(3), "Bandeja al Medio", "Aprovecha la duda de comunicación entre ambos defensores.", 0.55f)
            )
            "Voleas" -> listOf(
                CourtZoneHeat("red_pies", "Volea a los Pies (Red)", 0.50f, 0.56f, 36f, (baseMultiplier * 0.36f).toInt().coerceAtLeast(7), "Volea Bloqueada", "Bloqueo cortado que cae muerto en los cordones del rival que sube.", 0.95f),
                CourtZoneHeat("reja_corta", "Volea Cortada a la Reja", 0.16f, 0.58f, 32f, (baseMultiplier * 0.32f).toInt().coerceAtLeast(6), "Volea de Revés", "Ángulo corto y abierto directo a la malla lateral a velocidad media.", 0.88f),
                CourtZoneHeat("esquina_volea", "Volea Profunda al Cristal", 0.82f, 0.85f, 20f, (baseMultiplier * 0.20f).toInt().coerceAtLeast(4), "Volea de Derecha", "Volea agresiva y pesada buscando el cristal de fondo sin rebote.", 0.72f),
                CourtZoneHeat("dormilona", "Dormilona en la Red", 0.50f, 0.52f, 12f, (baseMultiplier * 0.12f).toInt().coerceAtLeast(2), "Dejada Milimétrica", "Toque sutil para dejar la bola pegada a la red tras rebote rival.", 0.50f)
            )
            "Bajadas" -> listOf(
                CourtZoneHeat("bajada_centro", "Bajada Directa al Medio", 0.50f, 0.68f, 40f, (baseMultiplier * 0.40f).toInt().coerceAtLeast(8), "Bajada de Pared", "Golpe muy acelerado de arriba abajo buscando el hueco entre los dos rivales.", 0.95f),
                CourtZoneHeat("bajada_cruzada", "Bajada Cruzada al Cuerpo", 0.22f, 0.60f, 35f, (baseMultiplier * 0.35f).toInt().coerceAtLeast(7), "Bajada con Efecto", "Tiro rápido hacia el hombro no hábil del voleador cruzado.", 0.85f),
                CourtZoneHeat("bajada_reja", "Bajada con Ángulo a Reja", 0.80f, 0.64f, 25f, (baseMultiplier * 0.25f).toInt().coerceAtLeast(5), "Bajada Cortada", "Sorprende con una trayectoria diagonal hacia la malla lateral.", 0.70f)
            )
            else -> listOf(
                CourtZoneHeat("reja_izq", "Reja Cruzada Izquierda", 0.16f, 0.62f, 28f, (baseMultiplier * 0.28f).toInt().coerceAtLeast(9), "Víbora / Volea", "Zona con mayor efectividad. El bote en la malla descoloca al rival.", 0.95f),
                CourtZoneHeat("x3_out", "Salida por 3 Metros", 0.06f, 0.42f, 22f, (baseMultiplier * 0.22f).toInt().coerceAtLeast(7), "Remate x3", "Remate definitivo liftado con impacto alto que sale de pista.", 0.88f),
                CourtZoneHeat("doble_pared", "Doble Pared Fondo", 0.20f, 0.90f, 19f, (baseMultiplier * 0.19f).toInt().coerceAtLeast(6), "Bandeja Profunda", "Bolas rasantes que tocan cristal de fondo y lateral sin levantarse.", 0.78f),
                CourtZoneHeat("centro_t", "Pasillo Central / La T", 0.50f, 0.78f, 16f, (baseMultiplier * 0.16f).toInt().coerceAtLeast(5), "Bajada de Pared", "Genera indecisión entre los rivales en el centro de la pista.", 0.70f),
                CourtZoneHeat("red_pies", "Volea a los Pies", 0.50f, 0.56f, 15f, (baseMultiplier * 0.15f).toInt().coerceAtLeast(4), "Volea Definitiva", "Volea rápida que cae antes de que el defensor pueda armar el golpe.", 0.65f)
            )
        }
    }

    var selectedZoneId by remember { mutableStateOf<String?>(courtZones.firstOrNull()?.id) }
    val activeZone = courtZones.find { it.id == selectedZoneId } ?: courtZones.first()

    // Pulse animation for selected zone target
    val infiniteTransition = rememberInfiniteTransition(label = "heatmap_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("padel_court_heatmap_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Subtitle
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5722).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Mapa de Calor en Pista",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zonas de caída de puntos ganadores",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF5722).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "FIP 10x20m",
                        color = Color(0xFFFF5722),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Stroke Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Todos", "Remates", "Bandejas", "Voleas", "Bajadas").forEach { filter ->
                    val isSelected = selectedStrokeFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable {
                                selectedStrokeFilter = filter
                                selectedZoneId = null
                            }
                            .padding(vertical = 7.dp)
                            .testTag("filter_stroke_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Perspective Filter: Mis Puntos vs Rivales vs Ambos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Mis Puntos Ganadores", "Puntos de Rivales", "Mapa Conjunto").forEachIndexed { idx, title ->
                    val isSelected = selectedPerspective == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .clickable { selectedPerspective = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Padel Court Canvas with Heatmap Gradient Clusters
            val isDark = MaterialTheme.colorScheme.surface.let {
                val lum = 0.299 * it.red + 0.587 * it.green + 0.114 * it.blue
                lum < 0.5
            }
            val courtBlue = if (isDark) Color(0xFF0D47A1) else Color(0xFF1976D2)
            val courtLinesColor = Color.White.copy(alpha = 0.85f)
            val glassWallColor = if (isDark) Color(0xFF64B5F6).copy(alpha = 0.4f) else Color(0xFF90CAF9)
            val meshColor = if (isDark) Color(0xFFB0BEC5).copy(alpha = 0.35f) else Color(0xFF78909C).copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(courtZones) {
                            detectTapGestures { tapOffset ->
                                // Find closest zone to tap
                                val width = size.width
                                val height = size.height
                                val courtLeft = 32.dp.toPx()
                                val courtRight = width - 32.dp.toPx()
                                val courtTop = 14.dp.toPx()
                                val courtBottom = height - 14.dp.toPx()
                                val courtW = courtRight - courtLeft
                                val courtH = courtBottom - courtTop

                                val tappedZone = courtZones.minByOrNull { zone ->
                                    val zX = courtLeft + zone.normalizedX * courtW
                                    val zY = courtTop + zone.normalizedY * courtH
                                    val dx = tapOffset.x - zX
                                    val dy = tapOffset.y - zY
                                    dx * dx + dy * dy
                                }
                                if (tappedZone != null) {
                                    selectedZoneId = tappedZone.id
                                }
                            }
                        }
                        .testTag("heatmap_court_canvas")
                ) {
                    val width = size.width
                    val height = size.height

                    // Dimensions of the padel court (ratio 1:2)
                    val courtLeft = 32.dp.toPx()
                    val courtRight = width - 32.dp.toPx()
                    val courtTop = 14.dp.toPx()
                    val courtBottom = height - 14.dp.toPx()
                    val courtW = courtRight - courtLeft
                    val courtH = courtBottom - courtTop

                    // 1. Draw Court Turf Background
                    drawRoundRect(
                        color = courtBlue,
                        topLeft = Offset(courtLeft, courtTop),
                        size = Size(courtW, courtH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // 2. Draw Glass Back Walls & Side Glass
                    val wallStroke = 3.dp.toPx()
                    // Top Back Wall (Cristal Superior)
                    drawLine(
                        color = glassWallColor,
                        start = Offset(courtLeft - 4.dp.toPx(), courtTop),
                        end = Offset(courtRight + 4.dp.toPx(), courtTop),
                        strokeWidth = wallStroke + 2f,
                        cap = StrokeCap.Round
                    )
                    // Bottom Back Wall (Cristal Inferior)
                    drawLine(
                        color = glassWallColor,
                        start = Offset(courtLeft - 4.dp.toPx(), courtBottom),
                        end = Offset(courtRight + 4.dp.toPx(), courtBottom),
                        strokeWidth = wallStroke + 2f,
                        cap = StrokeCap.Round
                    )
                    // Lateral Glass sections (3m from back wall)
                    val lateralGlassLen = courtH * 0.25f
                    // Top Left Lateral Glass
                    drawLine(glassWallColor, Offset(courtLeft, courtTop), Offset(courtLeft, courtTop + lateralGlassLen), wallStroke)
                    // Top Right Lateral Glass
                    drawLine(glassWallColor, Offset(courtRight, courtTop), Offset(courtRight, courtTop + lateralGlassLen), wallStroke)
                    // Bottom Left Lateral Glass
                    drawLine(glassWallColor, Offset(courtLeft, courtBottom), Offset(courtLeft, courtBottom - lateralGlassLen), wallStroke)
                    // Bottom Right Lateral Glass
                    drawLine(glassWallColor, Offset(courtRight, courtBottom), Offset(courtRight, courtBottom - lateralGlassLen), wallStroke)

                    // 3. Draw Lateral Metallic Mesh / Rejas (dotted/pattern lines)
                    val meshPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    // Left Mesh
                    drawLine(
                        color = meshColor,
                        start = Offset(courtLeft, courtTop + lateralGlassLen),
                        end = Offset(courtLeft, courtBottom - lateralGlassLen),
                        strokeWidth = 2.5.dp.toPx(),
                        pathEffect = meshPathEffect
                    )
                    // Right Mesh
                    drawLine(
                        color = meshColor,
                        start = Offset(courtRight, courtTop + lateralGlassLen),
                        end = Offset(courtRight, courtBottom - lateralGlassLen),
                        strokeWidth = 2.5.dp.toPx(),
                        pathEffect = meshPathEffect
                    )

                    // 4. White Padel Lines (5cm official equivalent)
                    val lineStroke = 1.6.dp.toPx()
                    // Court Perimeter
                    drawRect(
                        color = courtLinesColor,
                        topLeft = Offset(courtLeft, courtTop),
                        size = Size(courtW, courtH),
                        style = Stroke(width = lineStroke)
                    )

                    // Top Service Line (3m from back wall)
                    val serviceTopY = courtTop + (courtH * 0.30f)
                    drawLine(courtLinesColor, Offset(courtLeft, serviceTopY), Offset(courtRight, serviceTopY), lineStroke)

                    // Bottom Service Line (3m from back wall)
                    val serviceBottomY = courtBottom - (courtH * 0.30f)
                    drawLine(courtLinesColor, Offset(courtLeft, serviceBottomY), Offset(courtRight, serviceBottomY), lineStroke)

                    // Net Line (Center)
                    val netY = courtTop + (courtH * 0.5f)
                    // Net band with shadow
                    drawLine(
                        color = Color.White,
                        start = Offset(courtLeft - 6.dp.toPx(), netY),
                        end = Offset(courtRight + 6.dp.toPx(), netY),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Net posts
                    drawCircle(Color(0xFFFFD54F), 3.5.dp.toPx(), Offset(courtLeft - 6.dp.toPx(), netY))
                    drawCircle(Color(0xFFFFD54F), 3.5.dp.toPx(), Offset(courtRight + 6.dp.toPx(), netY))

                    // Center Line (La T de Saque) - Top side
                    val centerX = courtLeft + (courtW * 0.5f)
                    drawLine(courtLinesColor, Offset(centerX, serviceTopY), Offset(centerX, netY), lineStroke)
                    // Center Line - Bottom side
                    drawLine(courtLinesColor, Offset(centerX, netY), Offset(centerX, serviceBottomY), lineStroke)

                    // 5. Draw Heat Clusters (Radial Gradients based on Zone Frequencies)
                    courtZones.forEach { zone ->
                        val zX = courtLeft + zone.normalizedX * courtW
                        val zY = courtTop + zone.normalizedY * courtH
                        val isSelected = zone.id == selectedZoneId

                        val baseRadius = (32.dp.toPx() * zone.heatLevel).coerceIn(24.dp.toPx(), 48.dp.toPx())
                        val glowRadius = if (isSelected) baseRadius * pulseScale else baseRadius

                        val centerColor = when {
                            zone.heatLevel >= 0.85f -> Color(0xFFFF3D00).copy(alpha = 0.75f)
                            zone.heatLevel >= 0.70f -> Color(0xFFFF9100).copy(alpha = 0.70f)
                            else -> Color(0xFFFFD600).copy(alpha = 0.65f)
                        }

                        // Outer Heat Halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(centerColor, centerColor.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(zX, zY),
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = Offset(zX, zY)
                        )

                        // Core Hot Point
                        drawCircle(
                            color = if (isSelected) Color.White else Color(0xFFFFD54F),
                            radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                            center = Offset(zX, zY)
                        )

                        // Target Indicator Ring when selected
                        if (isSelected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = 13.dp.toPx() * pulseScale,
                                center = Offset(zX, zY),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }

                    // 6. Draw Zone Percentage Badges on the Court
                    val textPaint = Paint().apply {
                        textSize = 20f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }

                    courtZones.forEach { zone ->
                        val zX = courtLeft + zone.normalizedX * courtW
                        val zY = courtTop + zone.normalizedY * courtH
                        val isSelected = zone.id == selectedZoneId

                        textPaint.color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.argb(230, 255, 235, 59)
                        val text = "${zone.relativePercentage.toInt()}%"
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            zX,
                            zY - 8.dp.toPx(),
                            textPaint
                        )
                    }

                    // Court Labels (Net / Cristal / Reja)
                    val labelPaint = Paint().apply {
                        textSize = 18f
                        isAntiAlias = true
                        color = android.graphics.Color.argb(160, 255, 255, 255)
                        textAlign = Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText("RED", courtLeft + courtW * 0.5f, netY - 5f, labelPaint)
                    drawContext.canvas.nativeCanvas.drawText("CRISTAL FONDO", courtLeft + courtW * 0.5f, courtBottom - 6f, labelPaint)
                    drawContext.canvas.nativeCanvas.drawText("CRISTAL RIVAL", courtLeft + courtW * 0.5f, courtTop + 14f, labelPaint)
                }
            }

            // Quick zone tap pills below court
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                courtZones.forEach { zone ->
                    val isSelected = zone.id == selectedZoneId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFFFF5722)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedZoneId = zone.id }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = zone.name.substringBefore("(").trim(),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            // Selected Zone Inspection Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.35f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3D00))
                            )
                            Text(
                                text = activeZone.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF5722).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${activeZone.relativePercentage.toInt()}% Puntos Ganadores",
                                color = Color(0xFFFF5722),
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
                            Text("Total Ganadores", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("${activeZone.winningPointsCount} pts", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Golpe Dominante", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(activeZone.dominantStroke, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Efectividad", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Muy Alta 🔥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9100))
                        }
                    }

                    // Tactical tip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = activeZone.tacticalTip,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Heat Intensity Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intensidad de Caída:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFD600)))
                        Text("Moderada (10-18%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9100)))
                        Text("Alta (19-25%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3D00)))
                        Text("Crítica (>25%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3D00))
                    }
                }
            }
        }
    }
}
