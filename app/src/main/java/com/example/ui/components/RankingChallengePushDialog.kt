package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PadelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingChallengePushDialog(
    targetPlayerName: String,
    viewModel: PadelViewModel,
    onDismiss: () -> Unit
) {
    val challengeTypes = listOf("Disputa de Ranking (+50 pts)", "Partido Amistoso", "Revancha de Torneo")
    var selectedType by remember { mutableStateOf(challengeTypes[0]) }

    val venues = listOf("Club Central Pádel", "Premier Indoor Madrid", "Pistas Municipales")
    var selectedVenue by remember { mutableStateOf(venues[0]) }

    val times = listOf("Hoy 19:00", "Mañana 11:00", "Sábado 10:30")
    var selectedTime by remember { mutableStateOf(times[0]) }

    var customMessage by remember {
        mutableStateOf("¡Te reto a disputar los puntos del Ranking en $selectedVenue!")
    }
    var sendPushNotification by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.sendRankingChallenge(
                        targetPlayerName = targetPlayerName,
                        challengeType = selectedType,
                        customMessage = customMessage,
                        venue = selectedVenue,
                        proposedTime = selectedTime,
                        sendPush = sendPushNotification
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("confirm_challenge_push_btn")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar Desafío Push", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Desafiar a Ranking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Rival: $targetPlayerName", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Challenge Type Chips
                Text("Modalidad de Desafío:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    challengeTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                customMessage = if (type.contains("Ranking")) {
                                    "¡Te reto a disputar los puntos del Ranking en $selectedVenue!"
                                } else {
                                    "¿Jugamos un partido amistoso de pádel en $selectedVenue?"
                                }
                            },
                            label = { Text(type.take(16), fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Venue & Time Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pista / Club:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = selectedVenue,
                            onValueChange = { selectedVenue = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horario:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = selectedTime,
                            onValueChange = { selectedTime = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Message Text Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mensaje Personalizado:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challenge_message_input"),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Push Notification Toggle & Live Preview Box
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (sendPushNotification) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Notificación Push a $targetPlayerName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = sendPushNotification,
                        onCheckedChange = { sendPushNotification = it }
                    )
                }

                if (sendPushNotification) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.SportsTennis, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("VISTA PREVIA DE NOTIFICACIÓN PUSH", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "¡Nuevo Desafío! ${viewModel.loggedInName.value} te ha retado a un partido de pádel ($selectedType) en $selectedVenue.",
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}
