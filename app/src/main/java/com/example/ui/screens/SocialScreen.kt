package com.example.ui.screens

import android.widget.Toast
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.AutoAwesome
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import com.example.model.FriendlyChallenge
import com.example.model.SocialPlayer
import com.example.ui.components.PlayerProfileDialog
import com.example.ui.components.RankingChallengePushDialog
import com.example.ui.components.UserRankingList
import com.example.viewmodel.PadelViewModel

@Composable
fun SocialScreen(
    viewModel: PadelViewModel,
    onNavigateToMatch: () -> Unit,
    onNavigateToAnalyze: () -> Unit = {}
) {
    val players by viewModel.allPlayers.collectAsState()
    val challenges by viewModel.allChallenges.collectAsState()
    val loggedInName by viewModel.loggedInName.collectAsState()
    val myPublicAnalyses by viewModel.myPublicProfileAnalyses.collectAsState()
    val allAnalyses by viewModel.allAnalyses.collectAsState()
    val friendRequests by viewModel.cloudFriendRequests.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Feed IA, 1 = Ranking, 2 = Amigos, 3 = Desafíos
    var showChallengeDialog by remember { mutableStateOf(false) }
    var selectedPlayerName by remember { mutableStateOf("") }
    var challengeMessage by remember { mutableStateOf("") }
    var selectedPlayerForProfile by remember { mutableStateOf<SocialPlayer?>(null) }

    if (selectedPlayerForProfile != null) {
        val player = selectedPlayerForProfile!!
        val detailedProfile = viewModel.getPlayerDetailedProfile(player)
        PlayerProfileDialog(
            player = player,
            profile = detailedProfile,
            onDismiss = { selectedPlayerForProfile = null },
            onSendFriendRequest = {
                viewModel.sendFriendRequest(player.name)
            },
            onInviteToMatch = {
                viewModel.prefillMatchWithPlayer(player)
                selectedPlayerForProfile = null
                onNavigateToMatch()
            }
        )
    }

    var showShareAnalysisWithFriendDialog by remember { mutableStateOf(false) }
    var targetFriendName by remember { mutableStateOf("") }

    var showShareDialog by remember { mutableStateOf(false) }
    var shareContentTitle by remember { mutableStateOf("") }
    var shareContentBody by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Buttons (Feed IA, Ranking, Amigos, Desafíos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            // Tab 0: Feed IA
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_feed_ia"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Feed IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tab 1: Ranking
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_ranking"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ranking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tab 2: Amigos
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 2 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_friends"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (activeTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Amigos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (activeTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tab 3: Desafíos
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 3) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 3 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_challenges"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (activeTab == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Desafíos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (activeTab == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (activeTab == 0) {
            // Community Feed Section (AI-Analyzed Plays)
            Box(modifier = Modifier.weight(1f)) {
                CommunityFeedScreen(
                    viewModel = viewModel,
                    onNavigateBack = null,
                    onNavigateToAnalyze = onNavigateToAnalyze,
                    showTopBar = false
                )
            }
        } else if (activeTab == 1) {
            // User Ranking List with Firebase Cloud Sync, Global vs Friends Filter, Podium & Search
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserRankingList(
                    viewModel = viewModel,
                    onChallengePlayer = { player ->
                        selectedPlayerName = player.name
                        challengeMessage = "¡Reto a un partido amistoso de Pádel! ¿Jugamos?"
                        showChallengeDialog = true
                    }
                )
            }
        } else if (activeTab == 2) {
            // Friends List Section & Public Profile Showcase
            val friends = players.filter { it.isFriend }
            val otherPlayers = players.filter { !it.isFriend && !it.isCurrentUser }
            val pendingRequests = friendRequests.filter { it.status == "PENDIENTE" }

            var friendSubTab by remember { mutableStateOf(0) } // 0 = Mis Amigos, 1 = Descubrir Jugadores, 2 = Solicitudes
            var showAddFriendDialog by remember { mutableStateOf(false) }
            var newFriendName by remember { mutableStateOf("") }
            var newFriendLevel by remember { mutableStateOf("Intermedio") }
            var selectedAnalysisToShare by remember { mutableStateOf<com.example.model.VideoAnalysis?>(null) }
            var friendShareMessage by remember { mutableStateOf("¡Hola! Mira esta jugada que analicé con IA:") }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Friends Sub-navigation Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Pair("Mis Amigos (${friends.size})", 0),
                        Pair("Descubrir (${otherPlayers.size})", 1),
                        Pair("Solicitudes (${pendingRequests.size})", 2)
                    ).forEach { (label, idx) ->
                        val isSelected = friendSubTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { friendSubTab = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                // Public Profile Showcase Card
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_my_public_profile"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Mi Perfil Público de Jugadas IA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${myPublicAnalyses.size} publicadas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (myPublicAnalyses.isEmpty()) {
                            Text(
                                text = "Aún no has fijado jugadas en tu perfil público. Analiza un vídeo de pádel con IA y selecciona 'Publicar en Perfil Público' para que tus amigos las vean aquí.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        } else {
                            myPublicAnalyses.forEach { play ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "🎾 ${play.strokeType}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Surface(
                                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${play.score}/100 ⭐",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Enlace: ${play.playLink}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("PlayLink", play.playLink))
                                                Toast.makeText(context, "Enlace copiado: ${play.playLink}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copiar enlace",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (friendSubTab == 0) {
                    // Mis Amigos Tab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mis Amigos (${friends.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showAddFriendDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("add_friend_custom_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (friends.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Aún no tienes amigos agregados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pasa a la pestaña 'Descubrir' para enviar solicitudes de amistad por Firestore a otros jugadores o añádelos manualmente.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        friends.forEach { friend ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerForProfile = friend },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(friend.avatarColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = friend.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = friend.level,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = "•  ${friend.points} pts",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Profile inspect button
                                        IconButton(
                                            onClick = { selectedPlayerForProfile = friend },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Ver Perfil",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Challenge button
                                        IconButton(
                                            onClick = {
                                                selectedPlayerName = friend.name
                                                challengeMessage = "¡Hola! Te desafío a un partido de pádel oficial. ¿Cuándo jugamos?"
                                                showChallengeDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Desafiar",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Share Play / Link button
                                        IconButton(
                                            onClick = {
                                                targetFriendName = friend.name
                                                showShareAnalysisWithFriendDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Enviar jugada analizada",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Remove friend button
                                        IconButton(
                                            onClick = { viewModel.toggleFriendStatus(friend) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Quitar amigo",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (friendSubTab == 1) {
                    // Discover Players Tab (Firestore directory)
                    Text(
                        text = "Jugadores en la Comunidad (${otherPlayers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explora los perfiles de otros jugadores, consulta sus estadísticas y envíales una solicitud de amistad por Firebase Firestore.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    otherPlayers.forEach { player ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerForProfile = player },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(player.avatarColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(player.name.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Column {
                                        Text(player.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${player.level} • ${player.points} ELO • ${player.matchesPlayed} jugados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { selectedPlayerForProfile = player },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Perfil", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.sendFriendRequest(player.name)
                                            Toast.makeText(context, "Solicitud de amistad enviada a ${player.name} vía Firestore", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Pending Friend Requests Tab
                    Text(
                        text = "Solicitudes de Amistad Pendientes (${pendingRequests.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (pendingRequests.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
                                Text("No tienes solicitudes pendientes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Cuando otros jugadores te añadan por Firestore, aparecerán aquí para que aceptes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        pendingRequests.forEach { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Nivel: ${req.senderLevel} • Vía Firebase Firestore", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.respondToFriendRequest(req, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Aceptar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.respondToFriendRequest(req, false) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Rechazar", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Friend Dialog
            if (showAddFriendDialog) {
                AlertDialog(
                    onDismissRequest = { showAddFriendDialog = false },
                    title = { Text("Agregar Amigo de Pádel") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newFriendName,
                                onValueChange = { newFriendName = it },
                                label = { Text("Nombre del amigo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("add_friend_name_input")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Nivel de juego", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Iniciación", "Intermedio", "Avanzado", "Pro").forEach { lvl ->
                                        val isSelected = newFriendLevel == lvl
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { newFriendLevel = lvl }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lvl,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newFriendName.isNotBlank()) {
                                    viewModel.addCustomFriend(newFriendName, newFriendLevel)
                                    newFriendName = ""
                                    showAddFriendDialog = false
                                }
                            },
                            modifier = Modifier.testTag("add_friend_confirm_btn")
                        ) {
                            Text("Agregar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddFriendDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Share Analysis with Friend Dialog
            if (showShareAnalysisWithFriendDialog) {
                AlertDialog(
                    onDismissRequest = { showShareAnalysisWithFriendDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Enviar Jugada a $targetFriendName", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (allAnalyses.isEmpty()) {
                                Text(
                                    text = "Aún no tienes jugadas analizadas con IA. Puedes analizar un vídeo en la pestaña 'Vídeo IA' o compartir el enlace de la comunidad.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text("Selecciona la jugada analizada para compartir:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allAnalyses.take(4).forEach { analysis ->
                                        val isSelected = selectedAnalysisToShare?.id == analysis.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedAnalysisToShare = analysis },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "🎾 ${analysis.strokeType}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "Score: ${analysis.score}/100 ⭐ • ${analysis.levelTier}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = friendShareMessage,
                                onValueChange = { friendShareMessage = it },
                                label = { Text("Mensaje") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val chosen = selectedAnalysisToShare ?: allAnalyses.firstOrNull()
                                if (chosen != null) {
                                    viewModel.sendAnalysisLinkToFriends(chosen, listOf(targetFriendName), friendShareMessage)
                                } else {
                                    Toast.makeText(context, "Enlace enviado a $targetFriendName: padelsync://feed/connect", Toast.LENGTH_LONG).show()
                                }
                                showShareAnalysisWithFriendDialog = false
                            }
                        ) {
                            Text("Enviar Enlace")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showShareAnalysisWithFriendDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        } else {
            // Challenges Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tus Desafíos Activos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Alertas y Notificaciones Locales (Partidos programados y Desafíos aceptados)
                val notifLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Toast.makeText(context, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Notificaciones del Sistema (Pádel)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Alertas automáticas en la barra de estado cuando tus amigos aceptan un reto o cuando tienes partidos programados.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            lineHeight = 15.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.scheduleMatchReminder("Partido Liga Premier", "Pista 3 Central", "20:00")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Aviso Partido", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.notifyChallengeAccepted("Carlos Mendoza", "¡Carlos Mendoza ha aceptado tu desafío de pádel!")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Alerta Reto", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (challenges.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "No tienes desafíos pendientes. ¡Reta a un amigo desde la lista de ranking!",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    challenges.forEach { challenge ->
                        val isReceived = challenge.challengedName == loggedInName
                        val partnerName = if (isReceived) challenge.challengerName else challenge.challengedName

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("challenge_card_${challenge.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (challenge.status) {
                                    "Aceptado" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                    "Pendiente" -> MaterialTheme.colorScheme.surface
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (challenge.status) {
                                                        "Pendiente" -> MaterialTheme.colorScheme.tertiary
                                                        "Aceptado" -> MaterialTheme.colorScheme.primary
                                                        else -> Color.Gray
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isReceived) "Recibido de $partnerName" else "Enviado a $partnerName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Badge(
                                        containerColor = when (challenge.status) {
                                            "Pendiente" -> MaterialTheme.colorScheme.tertiaryContainer
                                            "Aceptado" -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = when (challenge.status) {
                                            "Pendiente" -> MaterialTheme.colorScheme.onTertiaryContainer
                                            "Aceptado" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    ) {
                                        Text(challenge.status, modifier = Modifier.padding(4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = challenge.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vence: ${challenge.dueDate}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )

                                    if (challenge.status == "Pendiente" && isReceived) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = { viewModel.rejectChallenge(challenge.id) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                                    .testTag("reject_challenge_btn_${challenge.id}")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.acceptChallenge(challenge.id, challenge.challengerName) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    .testTag("accept_challenge_btn_${challenge.id}")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    } else if (challenge.status == "Aceptado") {
                                        Button(
                                            onClick = {
                                                // Pre-populate match and navigate to Match screen!
                                                if (isReceived) {
                                                    viewModel.matchP1A = "Yo (Tú)"
                                                    viewModel.matchP1B = "Seleccionar Compañero"
                                                    viewModel.matchP2A = partnerName
                                                    viewModel.matchP2B = "Seleccionar Rival"
                                                } else {
                                                    viewModel.matchP1A = "Yo (Tú)"
                                                    viewModel.matchP1B = "Seleccionar Compañero"
                                                    viewModel.matchP2A = partnerName
                                                    viewModel.matchP2B = "Seleccionar Rival"
                                                }
                                                // Trigger screen transition
                                                onNavigateToMatch()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .testTag("play_challenge_btn_${challenge.id}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Jugar Partido", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Challenge Creation Dialog with Push Notification to Friends
    if (showChallengeDialog) {
        RankingChallengePushDialog(
            targetPlayerName = selectedPlayerName,
            viewModel = viewModel,
            onDismiss = { showChallengeDialog = false }
        )
    }

    // Share Achievements Simulation Overlay Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir en Redes Sociales", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Preview of the shared Padel Banner card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D21)) // Always dark card for sharing look
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PADEL TRACKER ACHIEVEMENTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                text = "🏆  $shareContentTitle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = shareContentBody,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Text("Selecciona una plataforma para compartir tu logro:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    // Social Share channels buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // WhatsApp Mock
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "¡Publicado en tu estado de WhatsApp!", Toast.LENGTH_SHORT).show()
                                    showShareDialog = false
                                }
                                .padding(8.dp)
                                .testTag("share_whatsapp")
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF25D366)), contentAlignment = Alignment.Center) {
                                Text("WA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Instagram Mock
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "¡Subido a tus historias de Instagram!", Toast.LENGTH_SHORT).show()
                                    showShareDialog = false
                                }
                                .padding(8.dp)
                                .testTag("share_instagram")
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE1306C)), contentAlignment = Alignment.Center) {
                                Text("IG", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Instagram", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Twitter / X Mock
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "¡Publicado en Twitter/X!", Toast.LENGTH_SHORT).show()
                                    showShareDialog = false
                                }
                                .padding(8.dp)
                                .testTag("share_twitter")
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                                Text("X", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("X / Twitter", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
