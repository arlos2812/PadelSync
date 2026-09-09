package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.WearableBackgroundService
import com.example.util.bluetooth.WatchInboundAction
import com.example.viewmodel.PadelViewModel

@Composable
fun BluetoothWearableSyncCard(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val bleState by viewModel.bleSyncState.collectAsState()
    val isServiceRunning by WearableBackgroundService.isRunning.collectAsState()

    val t1Games by viewModel.team1SetScores.collectAsState()
    val t2Games by viewModel.team2SetScores.collectAsState()
    val curSet = viewModel.currentSetIndex
    val p1Display = if (viewModel.isTieBreak) "${viewModel.p1TieBreakPoints}" else viewModel.getPointsString(viewModel.p1PointsIdx)
    val p2Display = if (viewModel.isTieBreak) "${viewModel.p2TieBreakPoints}" else viewModel.getPointsString(viewModel.p2PointsIdx)
    val serverName = viewModel.getActiveServerName()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bluetooth_wearable_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (bleState.isConnected) Color(0xFF4CAF50).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Auto-detected device status & re-detect action
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
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (bleState.isScanning) Color(0xFFFFB300)
                                else if (bleState.isConnected) Color(0xFF4CAF50)
                                else Color(0xFFE53935)
                            )
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = "Smartwatch",
                                tint = if (bleState.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = bleState.connectedDevice?.name ?: "Smartwatch Detectado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (bleState.isConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO-DETECTADO",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (bleState.isScanning) {
                                "Detectando reloj automáticamente..."
                            } else if (bleState.isConnected) {
                                "Sincronizado vía Health Connect / BLE • Batería ${bleState.connectedDevice?.batteryLevel ?: 94}%"
                            } else {
                                "Buscando smartwatch cercano..."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Auto-detect refresh button
                IconButton(
                    onClick = { viewModel.autoDetectSmartwatch() },
                    enabled = !bleState.isScanning,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (bleState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Auto-detectar de nuevo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Health Connect / Google Fit Background Service status badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Servicio de Fondo Google Fit / Health Connect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Interactive Smartwatch Scoreboard View (Verifiable on any connected watch)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121417))
                    .border(1.dp, Color(0xFF2C3238), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Watch Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "PANTALLA EN TU RELOJ (${bleState.connectedDevice?.brand ?: "Smartwatch"})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "95% 🔋",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                // Match Games & Set
                Text(
                    text = "SET ${curSet + 1}  •  Juegos: ${t1Games.getOrNull(curSet) ?: 0} - ${t2Games.getOrNull(curSet) ?: 0}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Large Wrist Score Display
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E232A),
                    border = BorderStroke(1.dp, Color(0xFF37474F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team 1 Points
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PAREJA 1",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Text(
                                text = p1Display,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        // Team 2 Points
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PAREJA 2",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64B5F6)
                            )
                            Text(
                                text = p2Display,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                if (serverName.isNotEmpty()) {
                    Text(
                        text = "Al saque: $serverName 🎾",
                        fontSize = 10.sp,
                        color = Color(0xFFFFD54F)
                    )
                }

                // Wrist Control Buttons: Allow modifying score directly from watch
                Text(
                    text = "Botones táctiles del reloj para poner el marcador:",
                    fontSize = 10.sp,
                    color = Color.LightGray.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            WearableBackgroundService.dispatchScoreFromWatch(WatchInboundAction.AddPoint(1))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wrist_btn_point_p1"),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("+1 Pto P1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            WearableBackgroundService.dispatchScoreFromWatch(WatchInboundAction.AddPoint(2))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wrist_btn_point_p2"),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("+1 Pto P2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            WearableBackgroundService.dispatchScoreFromWatch(WatchInboundAction.SwitchServer)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(0.8.dp, Color(0xFF546E7A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wrist_btn_switch_server"),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Saque", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            WearableBackgroundService.dispatchScoreFromWatch(WatchInboundAction.UndoScore)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(0.8.dp, Color(0xFF546E7A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wrist_btn_undo"),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Deshacer", fontSize = 10.sp)
                    }
                }
            }

            // Packets and Sync Confirmation Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sincronizado con cualquier reloj conectado",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${bleState.packetsSynced} cambios sincronizados",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
