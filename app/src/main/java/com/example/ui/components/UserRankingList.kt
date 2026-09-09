package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SocialPlayer
import com.example.viewmodel.PadelViewModel

enum class RankingFilterMode {
    GLOBAL,
    FRIENDS
}

/**
 * Reusable User Ranking List component consuming data from Firebase Firestore and local Room cache,
 * with fast filtering between Global and Friends, search, podium display, and friend toggling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRankingList(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier,
    onChallengePlayer: (SocialPlayer) -> Unit = {}
) {
    val players by viewModel.allPlayers.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val isFirebaseSyncing by viewModel.isFirebaseSyncing.collectAsState()
    val firebaseSyncMessage by viewModel.firebaseSyncMessage.collectAsState()

    var filterMode by remember { mutableStateOf(RankingFilterMode.GLOBAL) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter players based on tab and search
    val filteredPlayers = remember(players, filterMode, searchQuery) {
        val baseList = when (filterMode) {
            RankingFilterMode.GLOBAL -> players
            RankingFilterMode.FRIENDS -> players.filter { it.isFriend || it.isCurrentUser }
        }
        if (searchQuery.isBlank()) {
            baseList.sortedByDescending { it.points }
        } else {
            baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                .sortedByDescending { it.points }
        }
    }

    val friendsCount = remember(players) { players.count { it.isFriend } }
    val globalCount = remember(players) { players.size }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Firebase Cloud Firestore Status Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firebase_sync_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isFirebaseConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFirebaseConnected) Color(0xFF00E676)
                                else Color(0xFFFFB300)
                            )
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Firebase Firestore",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Firebase Firestore",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = firebaseSyncMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sync button
                IconButton(
                    onClick = { viewModel.syncRankingWithFirebase() },
                    enabled = !isFirebaseSyncing,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("sync_firebase_btn")
                ) {
                    if (isFirebaseSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Segmented Filter Tabs: Ranking Global vs Mis Amigos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Global Filter
            val isGlobalSelected = filterMode == RankingFilterMode.GLOBAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGlobalSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { filterMode = RankingFilterMode.GLOBAL }
                    .padding(vertical = 10.dp)
                    .testTag("filter_global_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isGlobalSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Global ($globalCount)",
                        fontSize = 12.sp,
                        fontWeight = if (isGlobalSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isGlobalSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Friends Filter
            val isFriendsSelected = filterMode == RankingFilterMode.FRIENDS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isFriendsSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { filterMode = RankingFilterMode.FRIENDS }
                    .padding(vertical = 10.dp)
                    .testTag("filter_friends_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isFriendsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mis Amigos ($friendsCount)",
                        fontSize = 12.sp,
                        fontWeight = if (isFriendsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFriendsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (filterMode == RankingFilterMode.GLOBAL) "Buscar jugador en ranking global..." else "Buscar en amigos...",
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ranking_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Top 3 Podium (Only when not actively searching and list has at least 3 players)
        if (searchQuery.isBlank() && filteredPlayers.size >= 3) {
            PodiumCard(
                first = filteredPlayers[0],
                second = filteredPlayers[1],
                third = filteredPlayers[2]
            )
        }

        // List Header with count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (filterMode == RankingFilterMode.GLOBAL) "Tabla General de Posiciones" else "Clasificación entre Amigos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${filteredPlayers.size} ${if (filteredPlayers.size == 1) "jugador" else "jugadores"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Ranking Items
        if (filteredPlayers.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (filterMode == RankingFilterMode.FRIENDS) Icons.Outlined.GroupAdd else Icons.Outlined.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (filterMode == RankingFilterMode.FRIENDS) "No tienes amigos agregados aún" else "No se encontraron jugadores",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (filterMode == RankingFilterMode.FRIENDS)
                            "Explora el Ranking Global y pulsa el icono de corazón para añadir jugadores a tu círculo de amigos."
                        else "Intenta con otro término de búsqueda.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    if (filterMode == RankingFilterMode.FRIENDS) {
                        Button(
                            onClick = { filterMode = RankingFilterMode.GLOBAL },
                            modifier = Modifier.padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ver Ranking Global", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredPlayers.forEachIndexed { index, player ->
                    RankingUserCard(
                        position = index + 1,
                        player = player,
                        onToggleFriend = { viewModel.toggleFriendStatus(player) },
                        onChallenge = { onChallengePlayer(player) }
                    )
                }
            }
        }
    }
}

/**
 * Top 3 Podium component celebrating the top community players.
 */
@Composable
fun PodiumCard(
    first: SocialPlayer,
    second: SocialPlayer,
    third: SocialPlayer
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("podium_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Líderes del Circuito",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place (Silver)
                PodiumColumn(
                    player = second,
                    rank = 2,
                    badgeColor = Color(0xFFB0BEC5),
                    stepHeight = 56.dp,
                    isWinner = false
                )

                // 1st Place (Gold - Taller)
                PodiumColumn(
                    player = first,
                    rank = 1,
                    badgeColor = Color(0xFFFFD700),
                    stepHeight = 76.dp,
                    isWinner = true
                )

                // 3rd Place (Bronze)
                PodiumColumn(
                    player = third,
                    rank = 3,
                    badgeColor = Color(0xFFCD7F32),
                    stepHeight = 44.dp,
                    isWinner = false
                )
            }
        }
    }
}

@Composable
fun PodiumColumn(
    player: SocialPlayer,
    rank: Int,
    badgeColor: Color,
    stepHeight: androidx.compose.ui.unit.Dp,
    isWinner: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(96.dp)
    ) {
        // Crown for #1
        if (isWinner) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Campeón",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(18.dp)
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(if (isWinner) 48.dp else 40.dp)
                .clip(CircleShape)
                .background(Color(player.avatarColor))
                .border(2.dp, badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = if (isWinner) 15.sp else 13.sp
            )
        }

        Text(
            text = player.name.substringBefore(" "),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${player.points} pts",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Step Pillar
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(stepHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.4f),
                            badgeColor.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = badgeColor
            )
        }
    }
}

/**
 * Single Player Row Card in the Ranking List.
 */
@Composable
fun RankingUserCard(
    position: Int,
    player: SocialPlayer,
    onToggleFriend: () -> Unit,
    onChallenge: () -> Unit
) {
    val isUser = player.isCurrentUser
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFB0BEC5)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ranking_card_${player.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isUser) 1.5.dp else 1.dp,
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Position + Avatar + Player Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Position badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(medalColor.copy(alpha = if (position <= 3) 0.25f else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$position",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (position <= 3) medalColor else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(player.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Info: Name + Badges + Level
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isUser) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("TÚ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Level Tag
                        val levelColor = when (player.level) {
                            "Pro" -> Color(0xFFE91E63)
                            "Avanzado" -> Color(0xFF2196F3)
                            "Intermedio" -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(levelColor.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = player.level,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = levelColor
                            )
                        }

                        // Record
                        Text(
                            text = "${player.matchesWon}V - ${player.matchesPlayed - player.matchesWon}D (${player.winRate.toInt()}%)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Right: Points + Friend Button + Challenge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Points
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "${player.points}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "pts ELO",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Friend Toggle (only for other players)
                if (!isUser) {
                    IconButton(
                        onClick = onToggleFriend,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("friend_btn_${player.id}")
                    ) {
                        Icon(
                            imageVector = if (player.isFriend) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (player.isFriend) "Eliminar de amigos" else "Añadir a amigos",
                            tint = if (player.isFriend) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Challenge button
                    IconButton(
                        onClick = onChallenge,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("challenge_btn_${player.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = "Desafiar a partido",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
