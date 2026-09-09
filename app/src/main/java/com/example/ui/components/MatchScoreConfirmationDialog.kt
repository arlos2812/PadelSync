package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadelMatch
import com.example.viewmodel.PadelViewModel

@Composable
fun MatchScoreConfirmationDialog(
    match: PadelMatch,
    viewModel: PadelViewModel,
    onDismiss: () -> Unit
) {
    var startTime by remember { mutableStateOf(match.scheduledStartTime.ifBlank { "18:30" }) }
    var endTime by remember { mutableStateOf(match.scheduledEndTime.ifBlank { "20:00" }) }
    var showHoursEdit by remember { mutableStateOf(false) }

    val confirmedPlayers = remember(match.confirmedByPlayers) {
        match.confirmedByPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    val players = listOf(
        Pair(match.player1A, "Pareja 1 (A)"),
        Pair(match.player1B, "Pareja 1 (B)"),
        Pair(match.player2A, "Pareja 2 (A)"),
        Pair(match.player2B, "Pareja 2 (B)")
    )

    val isFullyConfirmed = match.isScoreConfirmed || players.all { confirmedPlayers.contains(it.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Listo")
            }
        },
        dismissButton = {
            if (!isFullyConfirmed) {
                Button(
                    onClick = {
                        viewModel.confirmAllPlayersScore(match)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirm_all_players_score")
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Homologar los 4", fontWeight = FontWeight.Bold)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isFullyConfirmed) Icons.Default.Verified else Icons.Default.PendingActions,
                    contentDescription = null,
                    tint = if (isFullyConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800),
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (isFullyConfirmed) "Acta Homologada" else "Confirmación de Marcador",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Score card summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFullyConfirmed) Color(0xFF2E7D32).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Resultado Final Registrado:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.player1A} & ${match.player1B}",
                                fontSize = 13.sp,
                                fontWeight = if (match.winnerTeam == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (match.winnerTeam == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = match.setsTeam1,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.player2A} & ${match.player2B}",
                                fontSize = 13.sp,
                                fontWeight = if (match.winnerTeam == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (match.winnerTeam == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = match.setsTeam2,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Match Hours Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Horario del Encuentro",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "De $startTime a $endTime (${match.durationMinutes} min)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { showHoursEdit = !showHoursEdit },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showHoursEdit) Icons.Default.ExpandLess else Icons.Default.Edit,
                                    contentDescription = "Editar horas",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (showHoursEdit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Hora Inicio", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text("Hora Fin", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.updateMatchHours(match, startTime, endTime, match.durationMinutes)
                                    showHoursEdit = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Guardar Horario")
                            }
                        }
                    }
                }

                // Player Confirmation Signatures
                Text(
                    text = "Ratificación de Jugadores (${confirmedPlayers.size}/4)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                players.forEach { (playerName, role) ->
                    val isConfirmed = confirmedPlayers.contains(playerName)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConfirmed) Color(0xFF2E7D32).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConfirmed) Color(0xFF2E7D32).copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isConfirmed) Color(0xFF2E7D32) else Color(0xFFFF9800)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = playerName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$role • ${if (isConfirmed) "Marcador Confirmado" else "Pendiente de firma"}",
                                        fontSize = 10.sp,
                                        color = if (isConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isConfirmed) {
                                TextButton(
                                    onClick = {
                                        viewModel.confirmMatchScoreForPlayer(match, playerName)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Confirmar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Confirmado",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
