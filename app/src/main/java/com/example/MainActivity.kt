package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MatchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialScreen
import com.example.service.WearableBackgroundService
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.VideoAnalysisScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PadelViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launch background service for automatic wearable detection and score synchronization
        WearableBackgroundService.startService(this)

        setContent {
            val viewModel: PadelViewModel = viewModel()

            // Observe dynamic theme setting
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

            MyApplicationTheme(darkTheme = darkTheme) {
                if (isLoggedIn) {
                    MainAppLayout(viewModel)
                } else {
                    LoginScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: PadelViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val notificationBanner by viewModel.notificationBanner.collectAsState(initial = null)

    // Pulse animation for "Watch Linked" indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("app_scaffold"),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Section: App Logo & Brand
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsTennis,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PadelSync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    letterSpacing = (-0.5).sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "WATCH LINKED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Right Section: Dark Mode Toggle & Tab Context label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themeMode by viewModel.themeMode.collectAsState()
                            val isSystemDark = isSystemInDarkTheme()
                            val isDarkTheme = themeMode == "dark" || (themeMode == "system" && isSystemDark)

                            IconButton(
                                onClick = { viewModel.toggleThemeMode() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("btn_dark_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkTheme) "Modo Día" else "Modo Noche Exterior",
                                    tint = if (isDarkTheme) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "PARTIDO"
                                        1 -> "ESTADÍSTICAS"
                                        2 -> "VÍDEO IA"
                                        3 -> "COMUNIDAD"
                                        else -> "AJUSTES"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("nav_matches"),
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Partidos") },
                        label = { Text("Partidos", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("nav_stats"),
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Estadísticas") },
                        label = { Text("Estadísticas", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("nav_video_analysis"),
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Vídeo IA") },
                        label = { Text("Vídeo IA", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.testTag("nav_social"),
                        icon = { Icon(Icons.Default.Group, contentDescription = "Comunidad") },
                        label = { Text("Comunidad", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        modifier = Modifier.testTag("nav_settings"),
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> MatchScreen(viewModel)
                    1 -> StatsScreen(viewModel)
                    2 -> VideoAnalysisScreen(viewModel)
                    3 -> SocialScreen(
                        viewModel = viewModel,
                        onNavigateToMatch = { selectedTab = 0 },
                        onNavigateToAnalyze = { selectedTab = 2 }
                    )
                    4 -> SettingsScreen(viewModel)
                }
            }
        }

        // Animated overlay banner simulating push notifications HUD
        AnimatedVisibility(
            visible = notificationBanner != null,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(99f)
        ) {
            notificationBanner?.let { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 50.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .testTag("push_notification_hud"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (notif.type) {
                                        "challenge" -> MaterialTheme.colorScheme.tertiary
                                        "ranking" -> MaterialTheme.colorScheme.primary
                                        "sync" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (notif.type) {
                                    "challenge" -> Icons.Default.Group
                                    "ranking" -> Icons.Default.Star
                                    "sync" -> Icons.Default.CloudQueue
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (notif.type) {
                                    "challenge" -> MaterialTheme.colorScheme.onTertiary
                                    "ranking" -> MaterialTheme.colorScheme.onPrimary
                                    "sync" -> MaterialTheme.colorScheme.onSecondary
                                    else -> MaterialTheme.colorScheme.inverseOnSurface
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Text(
                                text = notif.body,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
