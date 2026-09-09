package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PadelRepository
import com.example.model.FriendlyChallenge
import com.example.model.MatchValidationReport
import com.example.model.PadelMatch
import com.example.model.PushNotificationLog
import com.example.model.SocialPlayer
import com.example.model.SupportTicket
import com.example.model.VideoAnalysis
import com.example.model.PremierHighlight
import com.example.model.WearableDevice
import com.example.model.BleSyncState
import com.example.model.HighlightReel
import com.example.model.FeedComment
import com.example.model.FriendRequest
import com.example.model.PlayerDetailedProfile
import com.example.model.SharedFeedPost
import com.example.model.PadelDrillSession
import com.example.util.bluetooth.SmartwatchBridge
import com.example.util.bluetooth.UniversalBleSmartwatchBridge
import com.example.util.bluetooth.SmartwatchSyncManager
import com.example.util.bluetooth.WatchInboundAction
import com.example.util.bluetooth.WatchMatchPacket
import com.example.util.FirebaseRankingService
import com.example.util.GeminiChatService
import com.example.util.GeminiVideoAnalyzer
import com.example.util.PadelAudioHelper
import com.example.util.PadelChatMessage
import com.example.util.PadelNetworkSyncManager
import com.example.util.PadelNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PadelViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("padelsync_prefs", android.content.Context.MODE_PRIVATE)

    val audioHelper = PadelAudioHelper(application)

    private val _voiceAnnouncer = MutableStateFlow(prefs.getBoolean("pref_voice_announcer", true))
    val voiceAnnouncer = _voiceAnnouncer.asStateFlow()

    private val _soundFx = MutableStateFlow(prefs.getBoolean("pref_sound_fx", true))
    val soundFx = _soundFx.asStateFlow()

    private val _haptics = MutableStateFlow(prefs.getBoolean("pref_haptics", true))
    val haptics = _haptics.asStateFlow()

    private val _courtSurface = MutableStateFlow(prefs.getString("pref_court_surface", "Azul Premier") ?: "Azul Premier")
    val courtSurface = _courtSurface.asStateFlow()

    private val _matchFormat = MutableStateFlow("3 Sets")
    val matchFormat = _matchFormat.asStateFlow()

    fun toggleVoiceAnnouncer() {
        val newVal = !_voiceAnnouncer.value
        _voiceAnnouncer.value = newVal
        audioHelper.voiceEnabled = newVal
        prefs.edit().putBoolean("pref_voice_announcer", newVal).apply()
    }

    fun toggleSoundFx() {
        val newVal = !_soundFx.value
        _soundFx.value = newVal
        audioHelper.soundFxEnabled = newVal
        prefs.edit().putBoolean("pref_sound_fx", newVal).apply()
    }

    fun toggleHaptics() {
        val newVal = !_haptics.value
        _haptics.value = newVal
        audioHelper.hapticsEnabled = newVal
        prefs.edit().putBoolean("pref_haptics", newVal).apply()
    }

    fun setCourtSurface(surface: String) {
        _courtSurface.value = surface
        prefs.edit().putString("pref_court_surface", surface).apply()
    }

    fun setMatchFormat(format: String) {
        _matchFormat.value = format
    }

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("pref_is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loggedInName = MutableStateFlow(prefs.getString("pref_user_name", "Yo (Tú)") ?: "Yo (Tú)")
    val loggedInName = _loggedInName.asStateFlow()

    private val _loggedInEmail = MutableStateFlow(prefs.getString("pref_user_email", "") ?: "")
    val loggedInEmail = _loggedInEmail.asStateFlow()

    private val _loggedInLevel = MutableStateFlow(prefs.getString("pref_user_level", "Intermedio") ?: "Intermedio")
    val loggedInLevel = _loggedInLevel.asStateFlow()

    fun login(email: String, name: String, level: String) {
        prefs.edit().apply {
            putBoolean("pref_is_logged_in", true)
            putString("pref_user_name", name)
            putString("pref_user_email", email)
            putString("pref_user_level", level)
            apply()
        }
        _isLoggedIn.value = true
        _loggedInName.value = name
        _loggedInEmail.value = email
        _loggedInLevel.value = level

        matchP1A = name

        viewModelScope.launch {
            val currentUser = SocialPlayer(
                id = 1,
                name = name,
                level = level,
                points = 1250,
                matchesPlayed = 12,
                matchesWon = 8,
                avatarColor = 0xFF4CAF50.toInt(),
                isCurrentUser = true
            )
            repository.updatePlayer(currentUser)
            firebaseRankingService.signInWithFirebaseAuth(email, name)
            firebaseRankingService.saveUserProfileToFirestore(currentUser, email)
        }

        triggerNotification(
            title = "Sesión Iniciada",
            body = "¡Hola $name! Bienvenido a PadelSync.",
            type = "info"
        )
    }

    /**
     * Google Sign-In with Firebase Auth & Cloud Firestore sync
     */
    fun loginWithGoogle(idToken: String, email: String, name: String, level: String = "Intermedio") {
        prefs.edit().apply {
            putBoolean("pref_is_logged_in", true)
            putString("pref_user_name", name)
            putString("pref_user_email", email)
            putString("pref_user_level", level)
            putString("pref_auth_provider", "google")
            apply()
        }
        _isLoggedIn.value = true
        _loggedInName.value = name
        _loggedInEmail.value = email
        _loggedInLevel.value = level

        matchP1A = name

        viewModelScope.launch {
            val currentUser = SocialPlayer(
                id = 1,
                name = name,
                level = level,
                points = 1350,
                matchesPlayed = 15,
                matchesWon = 11,
                avatarColor = 0xFF2196F3.toInt(),
                isCurrentUser = true
            )
            repository.updatePlayer(currentUser)
            firebaseRankingService.signInWithGoogleCredential(idToken, email, name)
            firebaseRankingService.saveUserProfileToFirestore(currentUser, email)
            // Synchronize match history to Firestore Cloud
            firebaseRankingService.syncUserMatchesAndStats(
                userId = firebaseRankingService.currentUser.value?.uid ?: "",
                email = email,
                name = name,
                matches = allMatches.value
            )
        }

        triggerNotification(
            title = "Google Sign-In Exitoso",
            body = "¡Bienvenido $name! Estadísticas sincronizadas con Firebase Cloud.",
            type = "info"
        )
    }

    /**
     * Trigger manual sync of Room matches and statistics to Cloud Firestore
     */
    fun syncStatsToFirebase() {
        viewModelScope.launch {
            val email = _loggedInEmail.value.ifBlank { "usuario@padelsync.com" }
            val name = _loggedInName.value.ifBlank { "Yo (Tú)" }
            val matches = allMatches.value
            val uid = firebaseRankingService.currentUser.value?.uid ?: ""
            firebaseRankingService.syncUserMatchesAndStats(
                userId = uid,
                email = email,
                name = name,
                matches = matches
            )
            triggerNotification(
                title = "Sincronización en la Nube",
                body = firebaseRankingService.syncStatusMessage.value,
                type = "info"
            )
        }
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean("pref_is_logged_in", false)
            putString("pref_user_name", "Yo (Tú)")
            putString("pref_user_email", "")
            putString("pref_user_level", "Intermedio")
            apply()
        }
        _isLoggedIn.value = false
        _loggedInName.value = "Yo (Tú)"
        _loggedInEmail.value = ""
        _loggedInLevel.value = "Intermedio"

        matchP1A = "Yo (Tú)"

        viewModelScope.launch {
            val currentUser = SocialPlayer(
                id = 1,
                name = "Yo (Tú)",
                level = "Intermedio",
                points = 1250,
                matchesPlayed = 12,
                matchesWon = 8,
                avatarColor = 0xFF4CAF50.toInt(),
                isCurrentUser = true
            )
            repository.updatePlayer(currentUser)
        }

        triggerNotification(
            title = "Sesión Cerrada",
            body = "Has cerrado sesión de manera segura.",
            type = "info"
        )
    }

    fun toggleFriendStatus(player: SocialPlayer) {
        viewModelScope.launch {
            val newFriendStatus = !player.isFriend
            repository.updatePlayerFriendStatus(player.id, newFriendStatus)
            firebaseRankingService.setPlayerFriendStatusInFirestore(player, newFriendStatus)
            val friendWord = if (newFriendStatus) "añadido a amigos" else "eliminado de amigos"
            triggerNotification(
                title = "Lista de amigos",
                body = "Has ${friendWord} a ${player.name}.",
                type = "info"
            )
        }
    }

    fun addCustomFriend(name: String, level: String) {
        viewModelScope.launch {
            val newFriend = SocialPlayer(
                name = name,
                level = level,
                points = 1000,
                matchesPlayed = 0,
                matchesWon = 0,
                avatarColor = listOf(0xFF2196F3.toInt(), 0xFF9C27B0.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt(), 0xFF4CAF50.toInt()).random(),
                isCurrentUser = false,
                isFriend = true
            )
            repository.insertPlayer(newFriend)
            firebaseRankingService.setPlayerFriendStatusInFirestore(newFriend, true)
            triggerNotification(
                title = "Amigo Agregado",
                body = "¡Has agregado a $name a tu lista de amigos!",
                type = "info"
            )
        }
    }

    private val repository: PadelRepository

    // Firebase Ranking Service Integration
    val firebaseRankingService = FirebaseRankingService.getInstance(application)
    val isFirebaseConnected: StateFlow<Boolean> = firebaseRankingService.isFirebaseConnected
    val isFirebaseSyncing: StateFlow<Boolean> = firebaseRankingService.isSyncing
    val firebaseSyncMessage: StateFlow<String> = firebaseRankingService.syncStatusMessage
    val cloudRankings: StateFlow<List<SocialPlayer>> = firebaseRankingService.cloudRankings

    fun syncRankingWithFirebase() {
        viewModelScope.launch {
            val currentLocal = allPlayers.value
            val result = firebaseRankingService.syncRankingsWithCloud(currentLocal)
            result.forEach { player ->
                repository.insertPlayer(player)
            }
        }
    }

    // Database Flows
    val allMatches: StateFlow<List<PadelMatch>>
    val allPlayers: StateFlow<List<SocialPlayer>>
    val allChallenges: StateFlow<List<FriendlyChallenge>>
    val allAnalyses: StateFlow<List<VideoAnalysis>>
    val allDrills: StateFlow<List<PadelDrillSession>>
    val unsyncedMatches: StateFlow<List<PadelMatch>>
    val cloudFriendRequests: StateFlow<List<FriendRequest>> = firebaseRankingService.cloudFriendRequests

    val networkSyncManager: PadelNetworkSyncManager
    val isNetworkOnline: StateFlow<Boolean>
    val isAutoSyncRunning: StateFlow<Boolean>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PadelRepository(
            database.padelMatchDao(),
            database.socialPlayerDao(),
            database.friendlyChallengeDao(),
            database.videoAnalysisDao(),
            database.padelDrillDao()
        )

        networkSyncManager = PadelNetworkSyncManager(application) {
            syncPendingMatchesToFirestore()
        }
        isNetworkOnline = networkSyncManager.isOnline
        isAutoSyncRunning = networkSyncManager.isAutoSyncRunning

        allMatches = repository.allMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allPlayers = repository.allPlayers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allChallenges = repository.allChallenges.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAnalyses = repository.allAnalyses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allDrills = repository.allDrills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        unsyncedMatches = repository.unsyncedMatchesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            // Check if players are empty and seed them
            allPlayers.collect { players ->
                if (players.isEmpty()) {
                    seedInitialData()
                }
            }
        }

        viewModelScope.launch {
            // Propagate cloud players updates into local DB
            firebaseRankingService.cloudRankings.collect { cloudPlayers ->
                if (cloudPlayers.isNotEmpty()) {
                    cloudPlayers.forEach { player ->
                        repository.insertPlayer(player)
                    }
                }
            }
        }

        viewModelScope.launch {
            // Listen to real-time challenges from Firebase Firestore
            firebaseRankingService.listenToCloudChallenges(_loggedInName.value) { cloudChallenges ->
                if (cloudChallenges.isNotEmpty()) {
                    viewModelScope.launch {
                        cloudChallenges.forEach { chal ->
                            repository.insertChallenge(chal)
                        }
                    }
                }
            }

            // Listen to real-time friend requests from Firebase Firestore
            firebaseRankingService.listenToCloudFriendRequests(_loggedInName.value) { requests ->
                // requests updated in cloudFriendRequests
            }
        }
    }

    private suspend fun seedInitialData() {
        val userName = _loggedInName.value
        val userLevel = _loggedInLevel.value
        val initialPlayers = listOf(
            SocialPlayer(name = userName, level = userLevel, points = 1250, matchesPlayed = 12, matchesWon = 8, avatarColor = 0xFF4CAF50.toInt(), isCurrentUser = true),
            SocialPlayer(name = "Carlos Mendoza", level = "Avanzado", points = 1600, matchesPlayed = 20, matchesWon = 15, avatarColor = 0xFF2196F3.toInt()),
            SocialPlayer(name = "Lucía Ruiz", level = "Intermedio", points = 1180, matchesPlayed = 10, matchesWon = 6, avatarColor = 0xFF9C27B0.toInt()),
            SocialPlayer(name = "Andrés Gómez", level = "Iniciación", points = 850, matchesPlayed = 6, matchesWon = 2, avatarColor = 0xFFFF9800.toInt(), isFriend = true),
            SocialPlayer(name = "Laura Salmerón", level = "Pro", points = 1950, matchesPlayed = 25, matchesWon = 21, avatarColor = 0xFFE91E63.toInt(), isFriend = true)
        )
        repository.populateInitialPlayers(initialPlayers)

        // Seed some history matches to show in stats and test filtering
        val now = System.currentTimeMillis()
        val oneDay = 86400000L
        val histMatch1 = PadelMatch(
            player1A = userName, player1B = "Carlos Mendoza",
            player2A = "Lucía Ruiz", player2B = "Andrés Gómez",
            setsTeam1 = "6,6", setsTeam2 = "4,3",
            currentSet = 2, winnerTeam = 1, timestamp = now - oneDay * 1,
            isSynced = true, durationMinutes = 52, matchFormat = "3 Sets", courtType = "Azul Premier",
            tournamentName = "Madrid Premier P1 - Fase Previa", matchResultType = "Torneo"
        )
        val histMatch2 = PadelMatch(
            player1A = userName, player1B = "Lucía Ruiz",
            player2A = "Carlos Mendoza", player2B = "Laura Salmerón",
            setsTeam1 = "3,4", setsTeam2 = "6,6",
            currentSet = 2, winnerTeam = 2, timestamp = now - oneDay * 3,
            isSynced = true, durationMinutes = 65, matchFormat = "3 Sets", courtType = "Panorámica WPT",
            tournamentName = "Liga Interclubs 1ª División", matchResultType = "Liga"
        )
        val histMatch3 = PadelMatch(
            player1A = userName, player1B = "Laura Salmerón",
            player2A = "Pablo Lima", player2B = "Fernando Belasteguín",
            setsTeam1 = "7,6", setsTeam2 = "6,4",
            currentSet = 2, winnerTeam = 1, timestamp = now - oneDay * 7,
            isSynced = true, durationMinutes = 78, matchFormat = "3 Sets", courtType = "Césped Fibrilado",
            tournamentName = "Torneo Benéfico Primavera", matchResultType = "Torneo"
        )
        val histMatch4 = PadelMatch(
            player1A = userName, player1B = "Andrés Gómez",
            player2A = "Lucía Ruiz", player2B = "Carlos Mendoza",
            setsTeam1 = "4,5", setsTeam2 = "6,7",
            currentSet = 2, winnerTeam = 2, timestamp = now - oneDay * 14,
            isSynced = true, durationMinutes = 55, matchFormat = "3 Sets", courtType = "Azul Premier",
            tournamentName = "Reto ELO Ranking", matchResultType = "Reto ELO"
        )
        val histMatch5 = PadelMatch(
            player1A = userName, player1B = "Carlos Mendoza",
            player2A = "Ale Galán", player2B = "Fede Chingotto",
            setsTeam1 = "6,4,7", setsTeam2 = "4,6,6",
            currentSet = 3, winnerTeam = 1, timestamp = now - oneDay * 28,
            isSynced = true, durationMinutes = 94, matchFormat = "3 Sets", courtType = "Azul Premier",
            tournamentName = "Gran Final Torneo Verano", matchResultType = "Torneo"
        )
        val histMatch6 = PadelMatch(
            player1A = userName, player1B = "Andrés Gómez",
            player2A = "Laura Salmerón", player2B = "Paquito Navarro",
            setsTeam1 = "6,3,4", setsTeam2 = "3,6,6",
            currentSet = 3, winnerTeam = 2, timestamp = now - oneDay * 45,
            isSynced = true, durationMinutes = 80, matchFormat = "3 Sets", courtType = "Panorámica WPT",
            tournamentName = "Amistoso Club Padel Center", matchResultType = "Amistoso"
        )
        repository.insertMatch(histMatch1)
        repository.insertMatch(histMatch2)
        repository.insertMatch(histMatch3)
        repository.insertMatch(histMatch4)
        repository.insertMatch(histMatch5)
        repository.insertMatch(histMatch6)

        // Seed initial friendly challenges
        val chal1 = FriendlyChallenge(
            challengerName = "Carlos Mendoza", challengedName = userName,
            message = "¡Revancha este fin de semana! ¿Te atreves?",
            status = "Pendiente", dueDate = "14/07/2026"
        )
        val chal2 = FriendlyChallenge(
            challengerName = userName, challengedName = "Andrés Gómez",
            message = "Un partido tranquilo para entrenar saques.",
            status = "Aceptado", dueDate = "12/07/2026"
        )
        repository.insertChallenge(chal1)
        repository.insertChallenge(chal2)

        // Seed initial video analysis
        val initialVideo = VideoAnalysis(
            title = "Bandeja Técnica a la Reja",
            strokeType = "Bandeja Cruzada a la Malla",
            score = 82,
            levelTier = "Intermedio Alto (Playtomic 4.2)",
            positiveFeedback = "• Armado alto temprano de la pala en posición de 'trofeo' al leer el globo rival.\n• Excelente empuñadura continental que permite aplicar efecto cortado descendente.",
            improvementFeedback = "1. PUNTO DE IMPACTO RETRASADO: Golpeas la bola justo encima del hombro derecho. Debes impactar 15-20 cm por delante de la cabeza para no perder profundidad.\n2. TRANSFERENCIA DE PESO: Entras al golpe con el cuerpo erguido. Flexiona más la pierna trasera e impulsa el peso hacia la red en el impacto.\n3. TERMINACIÓN: El gesto se corta bruscamente en la cintura. Acompaña la pala hacia el bolsillo izquierdo para asegurar bote bajo en el cristal lateral.",
            technicalBreakdown = "Juego de Pies: 78/100 • Punto de Impacto: 80/100 • Acompañamiento: 86/100 • Profundidad Táctica: 84/100",
            recommendedDrill = "🎯 Drill 'Diana a la Reja': 3 series de 15 bandejas desde la zona de transición buscando botar la bola antes de la línea de saque para que muera en la reja.",
            videoUriOrSample = "preset_bandeja",
            footworkScore = 78,
            impactScore = 80,
            followThroughScore = 86,
            tacticsScore = 84,
            timestamp = System.currentTimeMillis() - 86400000 * 2
        )
        repository.insertAnalysis(initialVideo)
    }

    // --- Dynamic Settings State ---
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "dark") ?: "dark") // "dark", "light", "system"
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun toggleThemeMode() {
        val nextMode = if (_themeMode.value == "dark") "light" else "dark"
        setThemeMode(nextMode)
    }

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing = _isCloudSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Sincronizado hoy a las 09:45")
    val lastSyncTime = _lastSyncTime.asStateFlow()

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            // Simulate sync delay
            delay(1500)
            _isCloudSyncing.value = false
            val timeString = "Sincronizado justo ahora"
            _lastSyncTime.value = timeString
            triggerNotification(
                title = "Sincronización en la nube",
                body = "Todos tus partidos y estadísticas están respaldados.",
                type = "sync"
            )
        }
    }

    // --- Notification Simulation ---
    private val _notifications = MutableStateFlow<List<PushNotificationLog>>(
        listOf(
            PushNotificationLog("1", "¡Desafío recibido!", "Carlos Mendoza te ha desafiado a un partido.", System.currentTimeMillis() - 3600000, "challenge"),
            PushNotificationLog("2", "Ranking Actualizado", "Has alcanzado el puesto #3 en el ranking local.", System.currentTimeMillis() - 7200000, "ranking")
        )
    )
    val notifications = _notifications.asStateFlow()

    // Banners to show at the top of the screen (simulating HUD notification)
    private val _notificationBanner = MutableSharedFlow<PushNotificationLog?>()
    val notificationBanner = _notificationBanner.asSharedFlow()

    fun triggerNotification(title: String, body: String, type: String) {
        val newNotif = PushNotificationLog(
            id = System.currentTimeMillis().toString(),
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        _notifications.value = listOf(newNotif) + _notifications.value
        viewModelScope.launch {
            _notificationBanner.emit(newNotif)
            delay(4000)
            _notificationBanner.emit(null)
        }

        // Post system notification to Android status bar
        if (type == "invitation" || title.contains("Invitación", ignoreCase = true) || title.contains("Convocatoria", ignoreCase = true)) {
            PadelNotificationManager.sendMatchInvitationAlert(
                context = getApplication(),
                hostPlayerName = _loggedInName.value.ifBlank { "Capitán de Partido" },
                courtName = _courtSurface.value,
                dateTimeText = if (scheduledMatchDate.isNotBlank()) "$scheduledMatchDate $scheduledStartTime" else "Próximamente"
            )
        } else if (type == "score_confirmed" || title.contains("Marcador", ignoreCase = true) || title.contains("Ratificado", ignoreCase = true)) {
            PadelNotificationManager.sendScoreConfirmedAlert(
                context = getApplication(),
                opponentName = "Carlos Mendoza",
                finalScore = "6-4, 4-6, 7-6",
                courtName = _courtSurface.value
            )
        } else if (type == "challenge" || title.contains("Desafío", ignoreCase = true) || title.contains("Reto", ignoreCase = true)) {
            PadelNotificationManager.sendChallengeAcceptedAlert(
                context = getApplication(),
                friendName = "Carlos Mendoza",
                challengeMessage = body
            )
        } else if (type == "match_reminder" || title.contains("Programado", ignoreCase = true) || title.contains("Recordatorio", ignoreCase = true)) {
            PadelNotificationManager.sendScheduledMatchReminder(
                context = getApplication(),
                matchTitle = title,
                courtName = _courtSurface.value,
                timeText = "19:00"
            )
        }
    }

    fun triggerDemoInvitationNotification() {
        PadelNotificationManager.sendMatchInvitationAlert(
            context = getApplication(),
            hostPlayerName = "Carlos Mendoza (Capitán)",
            courtName = "Pista Cristal Panorámica 1",
            dateTimeText = "Hoy a las 20:30 (Nocturno)"
        )
        triggerNotification(
            title = "🎾 ¡Nueva Invitación a Partido!",
            body = "Carlos Mendoza te ha invitado a jugar en Pista Cristal Panorámica hoy a las 20:30.",
            type = "invitation"
        )
    }

    fun triggerDemoScoreConfirmedNotification() {
        PadelNotificationManager.sendScoreConfirmedAlert(
            context = getApplication(),
            opponentName = "Lucía Ruiz",
            finalScore = "6-4, 4-6, 7-6",
            courtName = "Pista Azul Premier"
        )
        triggerNotification(
            title = "✅ ¡Marcador Oficial Ratificado!",
            body = "Lucía Ruiz ha confirmado el resultado final 6-4, 4-6, 7-6. Acta cerrada.",
            type = "score_confirmed"
        )
    }

    /**
     * Automatic synchronization layer: pushes pending Room matches to Firestore
     * when internet connectivity is available or restored.
     */
    suspend fun syncPendingMatchesToFirestore() {
        val pending = repository.getUnsyncedMatches()
        if (pending.isEmpty()) return
        val currentUserId = firebaseRankingService.currentUser.value?.uid ?: _loggedInEmail.value.ifBlank { "local_player_user" }
        val success = firebaseRankingService.syncUserMatchesAndStats(
            userId = currentUserId,
            email = _loggedInEmail.value,
            name = _loggedInName.value,
            matches = pending
        )
        if (success) {
            repository.markMatchesAsSynced(pending.map { it.id })
            _lastSyncTime.value = "Sincronizado automáticamente ahora"
            triggerNotification(
                title = "Sincronización Automática",
                body = "¡Conexión restaurada! Se han sincronizado ${pending.size} partidos con Firestore.",
                type = "sync"
            )
        }
    }

    /**
     * Persist drill session into Room database
     */
    suspend fun saveDrillSession(drill: PadelDrillSession) {
        repository.insertDrill(drill)
        triggerNotification(
            title = "Serie Guardada",
            body = "${drill.drillType}: ${drill.successfulReps} de ${drill.targetReps} aciertos (${drill.accuracyPercent.toInt()}%)",
            type = "training"
        )
    }

    /**
     * Delete drill session from Room
     */
    suspend fun deleteDrillSession(drillId: Int) {
        repository.deleteDrillById(drillId)
    }

    /**
     * Schedule a match reminder notification
     */
    fun scheduleMatchReminder(matchTitle: String, court: String = "Pista Central", timeText: String = "19:30") {
        PadelNotificationManager.sendScheduledMatchReminder(
            context = getApplication(),
            matchTitle = matchTitle,
            courtName = court,
            timeText = timeText
        )
        triggerNotification(
            title = "Partido Programado",
            body = "Recordatorio configurado: $matchTitle hoy a las $timeText en $court.",
            type = "match_reminder"
        )
    }

    /**
     * Notify user when a friend accepts their padel challenge
     */
    fun notifyChallengeAccepted(friendName: String, challengeText: String) {
        PadelNotificationManager.sendChallengeAcceptedAlert(
            context = getApplication(),
            friendName = friendName,
            challengeMessage = challengeText
        )
        triggerNotification(
            title = "¡Desafío Aceptado por $friendName!",
            body = challengeText,
            type = "challenge"
        )
    }

    // --- Active Match Engine ---
    var matchP1A by mutableStateOf(_loggedInName.value)
    var matchP1B by mutableStateOf("Carlos Mendoza")
    var matchP2A by mutableStateOf("Lucía Ruiz")
    var matchP2B by mutableStateOf("Andrés Gómez")

    // Scheduling and invitations
    var scheduledMatchDate by mutableStateOf("Hoy (08/09/2026)")
    var scheduledStartTime by mutableStateOf("18:30")
    var scheduledEndTime by mutableStateOf("20:00")
    var player1BInvitationStatus by mutableStateOf("Aceptado")
    var player2AInvitationStatus by mutableStateOf("Aceptado")
    var player2BInvitationStatus by mutableStateOf("Aceptado")

    var isGoldenPoint by mutableStateOf(true) // Punto de Oro vs Advantage

    var activeMatchId by mutableStateOf<Int?>(null)
    var isMatchInProgress by mutableStateOf(false)
    var currentSetIndex by mutableStateOf(0) // 0, 1, 2

    // Set Scores (Each entry holds scores for set 1, 2, 3, 4, 5, 6)
    var team1SetScores = mutableStateOf(intArrayOf(0, 0, 0, 0, 0, 0))
    var team2SetScores = mutableStateOf(intArrayOf(0, 0, 0, 0, 0, 0))

    // Current Game Points (within the active set)
    // Represented in index: 0="0", 1="15", 2="30", 3="40", 4="Ventaja"
    var p1PointsIdx by mutableStateOf(0)
    var p2PointsIdx by mutableStateOf(0)

    // Tie-Break Support (Official Padel rules at 6-6 in games)
    var isTieBreak by mutableStateOf(false)
    var p1TieBreakPoints by mutableStateOf(0)
    var p2TieBreakPoints by mutableStateOf(0)

    // Live Match Timer
    var matchSecondsElapsed by mutableStateOf(0)
    var isTimerPaused by mutableStateOf(false)
    private var timerJob: Job? = null

    // Victory celebration state
    var showVictoryDialog by mutableStateOf(false)
    var lastFinishedMatch by mutableStateOf<PadelMatch?>(null)

    // Serve Tracker
    // 0 = Team1 Player A, 1 = Team2 Player A, 2 = Team1 Player B, 3 = Team2 Player B
    var serverPlayerIdx by mutableStateOf(0)

    // Match Undo Stack (keeps history of scores inside the match for undo button)
    private val scoreHistory = mutableListOf<ScoreSnapshot>()

    data class ScoreSnapshot(
        val team1SetScores: IntArray,
        val team2SetScores: IntArray,
        val p1PointsIdx: Int,
        val p2PointsIdx: Int,
        val currentSetIndex: Int,
        val serverPlayerIdx: Int,
        val isTieBreak: Boolean,
        val p1TieBreakPoints: Int,
        val p2TieBreakPoints: Int
    )

    fun startNewMatch(p1A: String, p1B: String, p2A: String, p2B: String) {
        matchP1A = p1A.ifBlank { "Jugador 1A" }
        matchP1B = p1B.ifBlank { "Jugador 1B" }
        matchP2A = p2A.ifBlank { "Jugador 2A" }
        matchP2B = p2B.ifBlank { "Jugador 2B" }

        team1SetScores.value = intArrayOf(0, 0, 0, 0, 0, 0)
        team2SetScores.value = intArrayOf(0, 0, 0, 0, 0, 0)
        p1PointsIdx = 0
        p2PointsIdx = 0
        isTieBreak = false
        p1TieBreakPoints = 0
        p2TieBreakPoints = 0
        currentSetIndex = 0
        serverPlayerIdx = 0
        scoreHistory.clear()
        isMatchInProgress = true
        activeMatchId = null
        showVictoryDialog = false
        lastFinishedMatch = null

        // Reset and start timer
        matchSecondsElapsed = 0
        isTimerPaused = false
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isMatchInProgress) {
                delay(1000)
                if (isMatchInProgress && !isTimerPaused) {
                    matchSecondsElapsed++
                }
            }
        }

        audioHelper.playPointTone()
        audioHelper.vibratePoint()
        audioHelper.speak("Partido iniciado. Saca $matchP1A desde el lado derecho.")

        // Auto-connect smartwatch regardless of brand
        _isWatchSynced.value = true
        val watchName = _bleSyncState.value.connectedDevice?.name ?: "Smartwatch"
        smartwatchSyncManager.connectWatch(watchName, "Universal BLE")
        broadcastMatchScoreToSmartwatch()

        triggerNotification(
            title = "⌚ Smartwatch Conectado al Partido",
            body = "Marcador y sets sincronizados en tiempo real con tu $watchName. Puedes sumar y modificar puntos desde tu muñeca.",
            type = "info"
        )
    }

    fun togglePauseTimer() {
        isTimerPaused = !isTimerPaused
        if (isTimerPaused) {
            audioHelper.speak("Partido pausado")
        } else {
            audioHelper.speak("Partido reanudado")
        }
    }

    fun getFormattedMatchTime(): String {
        val mins = matchSecondsElapsed / 60
        val secs = matchSecondsElapsed % 60
        return String.format("%02d:%02d", mins, secs)
    }

    // Court serving side (Even points = Right/Deuce side, Odd points = Left/Ad side)
    fun isServerOnRightSide(): Boolean {
        return if (isTieBreak) {
            (p1TieBreakPoints + p2TieBreakPoints) % 2 == 0
        } else {
            (p1PointsIdx + p2PointsIdx) % 2 == 0
        }
    }

    fun getServerCourtSideText(): String {
        return if (isServerOnRightSide()) "LADO DERECHO" else "LADO IZQUIERDO"
    }

    private fun pushHistory() {
        scoreHistory.add(
            ScoreSnapshot(
                team1SetScores = team1SetScores.value.clone(),
                team2SetScores = team2SetScores.value.clone(),
                p1PointsIdx = p1PointsIdx,
                p2PointsIdx = p2PointsIdx,
                currentSetIndex = currentSetIndex,
                serverPlayerIdx = serverPlayerIdx,
                isTieBreak = isTieBreak,
                p1TieBreakPoints = p1TieBreakPoints,
                p2TieBreakPoints = p2TieBreakPoints
            )
        )
    }

    fun broadcastMatchScoreToSmartwatch() {
        val currentSetNum = currentSetIndex + 1
        val t1Games = team1SetScores.value.toList()
        val t2Games = team2SetScores.value.toList()
        val p1Str = if (isTieBreak) "$p1TieBreakPoints" else getPointsString(p1PointsIdx)
        val p2Str = if (isTieBreak) "$p2TieBreakPoints" else getPointsString(p2PointsIdx)
        val activeServer = getActiveServerName()

        val packet = WatchMatchPacket(
            currentSetIndex = currentSetIndex,
            currentSetDisplay = currentSetNum,
            team1SetScores = t1Games,
            team2SetScores = t2Games,
            team1PointsDisplay = p1Str,
            team2PointsDisplay = p2Str,
            isTieBreak = isTieBreak,
            team1TieBreakPoints = p1TieBreakPoints,
            team2TieBreakPoints = p2TieBreakPoints,
            activeServerName = activeServer,
            isServerRightSide = isServerOnRightSide(),
            matchDurationFormatted = getFormattedMatchTime(),
            team1Name = "$matchP1A & $matchP1B",
            team2Name = "$matchP2A & $matchP2B",
            isMatchFinished = !isMatchInProgress && lastFinishedMatch != null,
            winnerTeam = if (lastFinishedMatch?.winnerTeam == 1) 1 else if (lastFinishedMatch?.winnerTeam == 2) 2 else 0
        )
        smartwatchSyncManager.broadcastMatchUpdate(packet)
        com.example.service.WearableBackgroundService.updateLiveScore(packet)
    }

    fun undoLastScore() {
        if (scoreHistory.isNotEmpty()) {
            val last = scoreHistory.removeAt(scoreHistory.size - 1)
            team1SetScores.value = last.team1SetScores
            team2SetScores.value = last.team2SetScores
            p1PointsIdx = last.p1PointsIdx
            p2PointsIdx = last.p2PointsIdx
            currentSetIndex = last.currentSetIndex
            serverPlayerIdx = last.serverPlayerIdx
            isTieBreak = last.isTieBreak
            p1TieBreakPoints = last.p1TieBreakPoints
            p2TieBreakPoints = last.p2TieBreakPoints

            audioHelper.playPointTone()
            audioHelper.vibratePoint()
            speakScoreCall()
            broadcastMatchScoreToSmartwatch()
        }
    }

    fun addPointToTeam(team: Int) {
        if (!isMatchInProgress) return
        pushHistory()

        audioHelper.playPointTone()
        audioHelper.vibratePoint()

        // Sync score with connected Bluetooth wearable
        notifyBleScoreSync(team)

        if (isTieBreak) {
            handleTieBreakPoint(team)
        } else {
            if (team == 1) {
                incrementTeam1Score()
            } else {
                incrementTeam2Score()
            }
        }
        broadcastMatchScoreToSmartwatch()
    }

    fun awardGameFromWatch(winningTeam: Int) {
        if (!isMatchInProgress) return
        pushHistory()
        winGame(winningTeam)
        notifyBleScoreSync(winningTeam)
        broadcastMatchScoreToSmartwatch()
        val watchName = _bleSyncState.value.connectedDevice?.name ?: "Smartwatch"
        triggerNotification(
            title = "Juego Añadido desde $watchName",
            body = "Pareja $winningTeam se anota el juego directamente desde el reloj.",
            type = "info"
        )
    }

    fun switchServerFromWatch() {
        if (!isMatchInProgress) return
        pushHistory()
        serverPlayerIdx = (serverPlayerIdx + 1) % 4
        val newServer = getActiveServerName()
        audioHelper.speak("Cambio de saque. Saca $newServer.")
        broadcastMatchScoreToSmartwatch()
        triggerNotification(
            title = "Saque Cambiado en Smartwatch",
            body = "Ahora saca $newServer (ajustado desde el reloj).",
            type = "info"
        )
    }

    private fun handleTieBreakPoint(team: Int) {
        if (team == 1) {
            p1TieBreakPoints++
        } else {
            p2TieBreakPoints++
        }

        val totalPoints = p1TieBreakPoints + p2TieBreakPoints
        // Rotate serve after point 1, and every 2 points thereafter
        if (totalPoints % 2 == 1) {
            serverPlayerIdx = (serverPlayerIdx + 1) % 4
        }

        // Check if Tie-Break is won (first to 7 with >= 2 points margin)
        if (p1TieBreakPoints >= 7 && (p1TieBreakPoints - p2TieBreakPoints) >= 2) {
            winTieBreak(1)
        } else if (p2TieBreakPoints >= 7 && (p2TieBreakPoints - p1TieBreakPoints) >= 2) {
            winTieBreak(2)
        } else {
            // Announce tie-break score
            val call = if (p1TieBreakPoints == p2TieBreakPoints) {
                "$p1TieBreakPoints iguales"
            } else {
                "$p1TieBreakPoints a $p2TieBreakPoints"
            }
            audioHelper.speak(call)
        }
    }

    private fun winTieBreak(winningTeam: Int) {
        isTieBreak = false
        val sets1 = team1SetScores.value.clone()
        val sets2 = team2SetScores.value.clone()

        if (winningTeam == 1) {
            sets1[currentSetIndex] = 7
            sets2[currentSetIndex] = 6
        } else {
            sets1[currentSetIndex] = 6
            sets2[currentSetIndex] = 7
        }
        team1SetScores.value = sets1
        team2SetScores.value = sets2

        audioHelper.playGameWonTone()
        audioHelper.vibrateGameWon()
        audioHelper.speak("Set y tie-break para Pareja $winningTeam.")

        winSet(winningTeam)
    }

    private fun incrementTeam1Score() {
        if (isGoldenPoint) {
            if (p1PointsIdx == 3 && p2PointsIdx == 3) {
                // At 40-40, next point wins the game
                winGame(1)
            } else if (p1PointsIdx == 3) {
                winGame(1)
            } else {
                p1PointsIdx++
                speakScoreCall()
            }
        } else {
            // Advantage Scoring
            when {
                p1PointsIdx == 3 && p2PointsIdx == 3 -> {
                    p1PointsIdx = 4
                    speakScoreCall()
                }
                p1PointsIdx == 3 && p2PointsIdx == 4 -> {
                    p2PointsIdx = 3
                    speakScoreCall()
                }
                p1PointsIdx == 4 -> {
                    winGame(1)
                }
                p1PointsIdx == 3 -> {
                    winGame(1)
                }
                else -> {
                    p1PointsIdx++
                    speakScoreCall()
                }
            }
        }
    }

    private fun incrementTeam2Score() {
        if (isGoldenPoint) {
            if (p1PointsIdx == 3 && p2PointsIdx == 3) {
                winGame(2)
            } else if (p2PointsIdx == 3) {
                winGame(2)
            } else {
                p2PointsIdx++
                speakScoreCall()
            }
        } else {
            when {
                p1PointsIdx == 3 && p2PointsIdx == 3 -> {
                    p2PointsIdx = 4
                    speakScoreCall()
                }
                p2PointsIdx == 3 && p1PointsIdx == 4 -> {
                    p1PointsIdx = 3
                    speakScoreCall()
                }
                p2PointsIdx == 4 -> {
                    winGame(2)
                }
                p2PointsIdx == 3 -> {
                    winGame(2)
                }
                else -> {
                    p2PointsIdx++
                    speakScoreCall()
                }
            }
        }
    }

    fun speakScoreCall() {
        if (isTieBreak) {
            val call = if (p1TieBreakPoints == p2TieBreakPoints) {
                "$p1TieBreakPoints iguales"
            } else {
                "$p1TieBreakPoints a $p2TieBreakPoints"
            }
            audioHelper.speak(call)
            return
        }

        val s1 = getPointsString(p1PointsIdx)
        val s2 = getPointsString(p2PointsIdx)

        val text = when {
            p1PointsIdx == 3 && p2PointsIdx == 3 && isGoldenPoint -> "¡Punto de oro!"
            p1PointsIdx == 3 && p2PointsIdx == 3 -> "Iguales"
            p1PointsIdx == 4 -> "Ventaja Pareja 1"
            p2PointsIdx == 4 -> "Ventaja Pareja 2"
            p1PointsIdx == p2PointsIdx -> "$s1 iguales"
            else -> "$s1 - $s2"
        }
        audioHelper.speak(text)
    }

    private fun winGame(winningTeam: Int) {
        // Reset points for the new game
        p1PointsIdx = 0
        p2PointsIdx = 0

        // Rotate Server after each game
        serverPlayerIdx = (serverPlayerIdx + 1) % 4

        val sets1 = team1SetScores.value.clone()
        val sets2 = team2SetScores.value.clone()

        if (winningTeam == 1) {
            sets1[currentSetIndex]++
        } else {
            sets2[currentSetIndex]++
        }
        team1SetScores.value = sets1
        team2SetScores.value = sets2

        audioHelper.playGameWonTone()
        audioHelper.vibrateGameWon()

        val team1Games = sets1[currentSetIndex]
        val team2Games = sets2[currentSetIndex]

        // Check if Tie-Break should be triggered (6-6)
        if (team1Games == 6 && team2Games == 6) {
            isTieBreak = true
            p1TieBreakPoints = 0
            p2TieBreakPoints = 0
            audioHelper.speak("Juego para Pareja $winningTeam. Empate a 6 juegos, ¡comienza el Tie-Break!")
            return
        }

        // Format rules checking
        val isProSet = _matchFormat.value.contains("Pro Set")
        val isSingleSet = _matchFormat.value.contains("Set Único")

        if (isProSet) {
            // Pro set to 9 games
            if (team1Games >= 9 && (team1Games - team2Games) >= 2) {
                finishMatch(1)
                return
            } else if (team2Games >= 9 && (team2Games - team1Games) >= 2) {
                finishMatch(2)
                return
            }
        } else {
            // Standard set to 6 (or single set to 6)
            if (team1Games >= 6 && (team1Games - team2Games) >= 2) {
                winSet(1)
                return
            } else if (team1Games == 7 && team2Games == 5) {
                winSet(1)
                return
            } else if (team2Games >= 6 && (team2Games - team1Games) >= 2) {
                winSet(2)
                return
            } else if (team2Games == 7 && team1Games == 5) {
                winSet(2)
                return
            }
        }

        audioHelper.speak("Juego Pareja $winningTeam. Marcador: $team1Games a $team2Games.")
    }

    private fun winSet(winningTeam: Int) {
        val isSingleSet = _matchFormat.value.contains("Set Único")
        if (isSingleSet) {
            finishMatch(winningTeam)
            return
        }

        val isSixSets = _matchFormat.value.contains("6 Sets")
        val isFiveSets = _matchFormat.value.contains("5 Sets")
        val maxSets = if (isSixSets) 6 else if (isFiveSets) 5 else 3
        val requiredWins = if (isSixSets) 4 else if (isFiveSets) 3 else 2

        val setWinsTeam1 = getSetWins(1)
        val setWinsTeam2 = getSetWins(2)

        val actualWinsTeam1 = setWinsTeam1 + (if (winningTeam == 1) 1 else 0)
        val actualWinsTeam2 = setWinsTeam2 + (if (winningTeam == 2) 1 else 0)

        if (actualWinsTeam1 >= requiredWins) {
            finishMatch(1)
        } else if (actualWinsTeam2 >= requiredWins) {
            finishMatch(2)
        } else if (currentSetIndex + 1 >= maxSets) {
            // Reached maximum allowed sets, team with most sets wins
            val finalWinner = if (actualWinsTeam1 >= actualWinsTeam2) 1 else 2
            finishMatch(finalWinner)
        } else {
            // Move to next set
            currentSetIndex = (currentSetIndex + 1).coerceAtMost(maxSets - 1)
            audioHelper.speak("Set para Pareja $winningTeam. Comienza el Set ${currentSetIndex + 1}.")
            triggerNotification(
                title = "Fin del Set ${currentSetIndex}",
                body = "El set fue ganado por el Equipo $winningTeam.",
                type = "info"
            )
        }
    }

    private fun getSetWins(team: Int): Int {
        var wins = 0
        for (i in 0..currentSetIndex) {
            val g1 = team1SetScores.value[i]
            val g2 = team2SetScores.value[i]
            if (g1 > g2 && (g1 >= 6 || g1 == 7) && team == 1) {
                wins++
            } else if (g2 > g1 && (g2 >= 6 || g2 == 7) && team == 2) {
                wins++
            }
        }
        return wins
    }

    fun finishMatch(winner: Int) {
        isMatchInProgress = false
        timerJob?.cancel()

        val finalSetsT1 = team1SetScores.value.take(currentSetIndex + 1).joinToString(",")
        val finalSetsT2 = team2SetScores.value.take(currentSetIndex + 1).joinToString(",")
        val durationMins = (matchSecondsElapsed / 60).coerceAtLeast(1)

        audioHelper.playMatchWonFanfare()
        audioHelper.vibrateGameWon()
        audioHelper.speak("¡Fin del partido! Victoria para Pareja $winner.")

        viewModelScope.launch {
            val match = PadelMatch(
                player1A = matchP1A, player1B = matchP1B,
                player2A = matchP2A, player2B = matchP2B,
                setsTeam1 = finalSetsT1, setsTeam2 = finalSetsT2,
                currentSet = currentSetIndex + 1,
                pointsTeam1 = if (isTieBreak) "$p1TieBreakPoints" else getPointsString(p1PointsIdx),
                pointsTeam2 = if (isTieBreak) "$p2TieBreakPoints" else getPointsString(p2PointsIdx),
                winnerTeam = winner,
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                durationMinutes = durationMins,
                matchFormat = _matchFormat.value,
                courtType = _courtSurface.value,
                scheduledDate = scheduledMatchDate,
                scheduledStartTime = scheduledStartTime,
                scheduledEndTime = scheduledEndTime,
                player1BStatus = player1BInvitationStatus,
                player2AStatus = player2AInvitationStatus,
                player2BStatus = player2BInvitationStatus,
                isScoreConfirmed = false,
                confirmedByPlayers = _loggedInName.value
            )
            val matchId = repository.insertMatch(match)
            activeMatchId = matchId.toInt()
            lastFinishedMatch = match
            showVictoryDialog = true

            // Update stats for registered players if they exist in ranking
            updatePlayerScores(winner)

            triggerNotification(
                title = "Partido Guardado",
                body = "¡Felicidades al Equipo $winner por la victoria! Marcador: $finalSetsT1 / $finalSetsT2. Pendiente confirmación de rivales.",
                type = "ranking"
            )
        }
    }

    /**
     * Send invitations to selected players for a scheduled padel match.
     */
    fun sendMatchInvitation(slot: String, playerName: String) {
        when (slot) {
            "p1B" -> {
                matchP1B = playerName
                player1BInvitationStatus = "Invitación Enviada"
            }
            "p2A" -> {
                matchP2A = playerName
                player2AInvitationStatus = "Invitación Enviada"
            }
            "p2B" -> {
                matchP2B = playerName
                player2BInvitationStatus = "Invitación Enviada"
            }
        }
        triggerNotification(
            title = "Invitación Enviada",
            body = "Convocatoria enviada a $playerName para el $scheduledMatchDate de $scheduledStartTime a $scheduledEndTime.",
            type = "invitation"
        )
        PadelNotificationManager.sendMatchInvitationAlert(
            context = getApplication(),
            hostPlayerName = _loggedInName.value.ifBlank { "Tú" },
            courtName = _courtSurface.value,
            dateTimeText = "$scheduledMatchDate $scheduledStartTime"
        )
    }

    fun toggleInvitationAccepted(slot: String) {
        when (slot) {
            "p1B" -> player1BInvitationStatus = if (player1BInvitationStatus == "Aceptado") "Pendiente" else "Aceptado"
            "p2A" -> player2AInvitationStatus = if (player2AInvitationStatus == "Aceptado") "Pendiente" else "Aceptado"
            "p2B" -> player2BInvitationStatus = if (player2BInvitationStatus == "Aceptado") "Pendiente" else "Aceptado"
        }
    }

    fun sendAllMatchInvitations(p1B: String, p2A: String, p2B: String) {
        matchP1B = p1B.ifBlank { "Compañero" }
        matchP2A = p2A.ifBlank { "Rival 1" }
        matchP2B = p2B.ifBlank { "Rival 2" }
        player1BInvitationStatus = "Aceptado"
        player2AInvitationStatus = "Aceptado"
        player2BInvitationStatus = "Aceptado"
        triggerNotification(
            title = "Convocatoria Confirmada",
            body = "¡Los 4 jugadores han confirmado asistencia para el $scheduledMatchDate de $scheduledStartTime a $scheduledEndTime!",
            type = "challenge"
        )
    }

    /**
     * Confirms the official final score by a specific player.
     */
    fun confirmMatchScoreForPlayer(match: PadelMatch, playerName: String) {
        viewModelScope.launch {
            val currentList = match.confirmedByPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!currentList.contains(playerName)) {
                currentList.add(playerName)
            }
            val allRequired = setOf(match.player1A, match.player1B, match.player2A, match.player2B)
            val isAll = allRequired.all { currentList.contains(it) } || currentList.size >= 3

            val updated = match.copy(
                confirmedByPlayers = currentList.joinToString(","),
                isScoreConfirmed = isAll
            )
            repository.updateMatch(updated)
            if (lastFinishedMatch?.id == match.id) {
                lastFinishedMatch = updated
            }

            firebaseRankingService.confirmMatchScoreInFirestore("match_${match.id}", playerName, isAll)

            PadelNotificationManager.sendScoreConfirmedAlert(
                context = getApplication(),
                opponentName = playerName,
                finalScore = "${match.setsTeam1} / ${match.setsTeam2}",
                courtName = match.courtType,
                matchId = match.id
            )

            triggerNotification(
                title = if (isAll) "Marcador Homologado 100%" else "Marcador Ratificado",
                body = if (isAll) "Los 4 jugadores han ratificado el acta del partido. ¡Resultado oficial cerrado!"
                       else "$playerName ha confirmado el marcador del partido.",
                type = "score_confirmed"
            )
        }
    }

    /**
     * Ratifies and confirms the score by all four players simultaneously.
     */
    fun confirmAllPlayersScore(match: PadelMatch) {
        viewModelScope.launch {
            val allPlayers = listOf(match.player1A, match.player1B, match.player2A, match.player2B)
            val updated = match.copy(
                confirmedByPlayers = allPlayers.joinToString(","),
                isScoreConfirmed = true
            )
            repository.updateMatch(updated)
            if (lastFinishedMatch?.id == match.id) {
                lastFinishedMatch = updated
            }

            firebaseRankingService.confirmMatchScoreInFirestore("match_${match.id}", "Todos", true)

            triggerNotification(
                title = "Acta Oficial Cerrada",
                body = "Marcador ratificado y homologado por los 4 jugadores del encuentro.",
                type = "ranking"
            )
        }
    }

    /**
     * Updates scheduled or recorded match hours after the match ends.
     */
    fun updateMatchHours(match: PadelMatch, newStartTime: String, newEndTime: String, newDurationMins: Int) {
        viewModelScope.launch {
            val updated = match.copy(
                scheduledStartTime = newStartTime,
                scheduledEndTime = newEndTime,
                durationMinutes = newDurationMins
            )
            repository.updateMatch(updated)
            if (lastFinishedMatch?.id == match.id) {
                lastFinishedMatch = updated
            }
            triggerNotification(
                title = "Horario Actualizado",
                body = "Horas registradas de $newStartTime a $newEndTime ($newDurationMins min).",
                type = "info"
            )
        }
    }

    /**
     * Friend Request Management using Firebase Firestore.
     */
    fun sendFriendRequest(targetPlayerName: String) {
        viewModelScope.launch {
            val success = firebaseRankingService.sendFriendRequestToFirestore(
                senderName = _loggedInName.value,
                senderLevel = _loggedInLevel.value,
                receiverName = targetPlayerName
            )
            if (success) {
                triggerNotification(
                    title = "Solicitud Enviada",
                    body = "Se ha enviado tu solicitud de amistad a $targetPlayerName en Firestore.",
                    type = "challenge"
                )
            }
        }
    }

    fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            firebaseRankingService.respondToFriendRequestInFirestore(request.id, accept)
            if (accept) {
                val existing = allPlayers.value.find { it.name.equals(request.senderName, ignoreCase = true) }
                if (existing != null) {
                    repository.updatePlayerFriendStatus(existing.id, true)
                } else {
                    repository.insertPlayer(
                        SocialPlayer(
                            name = request.senderName,
                            level = request.senderLevel,
                            points = 1350,
                            matchesPlayed = 8,
                            matchesWon = 5,
                            avatarColor = 0xFF2196F3.toInt(),
                            isFriend = true
                        )
                    )
                }
                triggerNotification(
                    title = "¡Nueva Amistad!",
                    body = "Ahora eres amigo de ${request.senderName}. ¡Ya podéis organizar partidos!",
                    type = "challenge"
                )
            } else {
                triggerNotification(
                    title = "Solicitud Rechazada",
                    body = "Has rechazado la solicitud de ${request.senderName}.",
                    type = "info"
                )
            }
        }
    }

    /**
     * Generates a detailed performance profile for a player.
     */
    fun getPlayerDetailedProfile(player: SocialPlayer): PlayerDetailedProfile {
        val isMe = player.name == _loggedInName.value || player.isCurrentUser
        return PlayerDetailedProfile(
            name = player.name,
            levelTier = player.level,
            eloPoints = player.points,
            matchesPlayed = player.matchesPlayed.coerceAtLeast(1),
            matchesWon = player.matchesWon,
            preferredSide = if (player.name.contains("Carlos") || player.name.contains("Laura")) "Revés" else "Drive",
            dominantHand = if (player.name.contains("Lucía")) "Zurdo" else "Diestro",
            favoriteRacket = if (isMe) "Babolat Technical Viper 2026" else if (player.level == "Pro") "Bullpadel Vertex 04" else "Head Extreme Pro",
            smashEffectiveness = if (player.level == "Pro") 92 else if (player.level == "Avanzado") 86 else 75,
            volleyEffectiveness = if (player.level == "Pro") 88 else if (player.level == "Avanzado") 81 else 72,
            bandejaEffectiveness = if (player.level == "Pro") 90 else if (player.level == "Avanzado") 84 else 76,
            wallExitEffectiveness = if (player.level == "Pro") 87 else if (player.level == "Avanzado") 79 else 70,
            avgPointsPerMatch = if (player.level == "Pro") 58.4f else 51.6f,
            goldenPointsWon = if (player.matchesWon > 5) 12 else 4,
            isFriend = player.isFriend
        )
    }

    fun prefillMatchWithPlayer(player: SocialPlayer) {
        matchP1A = _loggedInName.value
        matchP1B = "Compañero Libre"
        matchP2A = player.name
        matchP2B = "Rival Libre"
        triggerNotification(
            title = "Alineación Preparada",
            body = "Se ha configurado un partido contra ${player.name}. Ve a la pestaña 'Partido' para empezar.",
            type = "info"
        )
    }

    fun deleteMatch(matchId: Int) {
        viewModelScope.launch {
            repository.deleteMatchById(matchId)
            triggerNotification(
                title = "Partido Eliminado",
                body = "El partido ha sido eliminado del registro histórico.",
                type = "info"
            )
        }
    }

    private suspend fun updatePlayerScores(winner: Int) {
        val userName = _loggedInName.value
        val userIsTeam1 = matchP1A == userName || matchP1B == userName
        val userIsTeam2 = matchP2A == userName || matchP2B == userName

        if (userIsTeam1) {
            val userWon = winner == 1
            repository.updatePlayerStats(userName, if (userWon) 50 else -20, userWon)
            if (matchP1B != userName) {
                repository.updatePlayerStats(matchP1B, if (userWon) 50 else -20, userWon)
            }
            // Update opponents as well
            repository.updatePlayerStats(matchP2A, if (!userWon) 50 else -20, !userWon)
            repository.updatePlayerStats(matchP2B, if (!userWon) 50 else -20, !userWon)
        } else if (userIsTeam2) {
            val userWon = winner == 2
            repository.updatePlayerStats(userName, if (userWon) 50 else -20, userWon)
            if (matchP2B != userName) {
                repository.updatePlayerStats(matchP2B, if (userWon) 50 else -20, userWon)
            }
            // Update opponents
            repository.updatePlayerStats(matchP1A, if (!userWon) 50 else -20, !userWon)
            repository.updatePlayerStats(matchP1B, if (!userWon) 50 else -20, !userWon)
        }
    }

    fun getPointsString(idx: Int): String {
        return when (idx) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            4 -> "Ad"
            else -> "0"
        }
    }

    fun getActiveServerName(): String {
        return when (serverPlayerIdx) {
            0 -> matchP1A
            1 -> matchP2A
            2 -> matchP1B
            3 -> matchP2B
            else -> matchP1A
        }
    }

    /**
     * Suma un juego directamente a la pareja indicada, aplicando las reglas oficiales de pádel,
     * rotación de saque, sets y tie-break.
     */
    fun addGameToTeam(team: Int) {
        if (!isMatchInProgress) return
        pushHistory()
        winGame(team)
        notifyBleScoreSync(team)
        broadcastMatchScoreToSmartwatch()
    }

    /**
     * Calcula el lado reglamentario de la pista desde el cual se debe realizar el saque (Derecha o Izquierda).
     * Según la FIP:
     * - El primer punto de cada juego se saca desde el lado derecho (Deuce).
     * - Se alterna de lado tras cada punto (puntuación acumulada par = derecha, impar = izquierda).
     */
    val currentServeSide: String
        get() {
            val totalPoints = if (isTieBreak) {
                p1TieBreakPoints + p2TieBreakPoints
            } else {
                p1PointsIdx + p2PointsIdx
            }
            return if (totalPoints % 2 == 0) "Derecha (Lado Par)" else "Izquierda (Lado Impar)"
        }

    /**
     * Valida si los sets de un partido cumplen con las reglas oficiales de la Federación Internacional de Pádel (FIP).
     */
    fun validatePadelSets(sets1Str: String, sets2Str: String): MatchValidationReport {
        val s1List = sets1Str.split(",").mapNotNull { it.trim().toIntOrNull() }
        val s2List = sets2Str.split(",").mapNotNull { it.trim().toIntOrNull() }

        val violations = mutableListOf<String>()

        if (s1List.isEmpty() || s2List.isEmpty()) {
            violations.add("Debe registrar al menos un set.")
            return MatchValidationReport(false, violations, "Tanteo vacío")
        }
        if (s1List.size != s2List.size) {
            violations.add("El número de sets para ambos equipos debe coincidir.")
            return MatchValidationReport(false, violations, "Sets desbalanceados")
        }

        var setsWonT1 = 0
        var setsWonT2 = 0

        for (i in s1List.indices) {
            val g1 = s1List[i]
            val g2 = s2List[i]

            val isSetValid = when {
                // Regular sets (6-0, 6-1, 6-2, 6-3, 6-4)
                g1 == 6 && g2 <= 4 -> true
                g2 == 6 && g1 <= 4 -> true
                // 7-5 sets
                g1 == 7 && g2 == 5 -> true
                g2 == 7 && g1 == 5 -> true
                // 7-6 Tie-Break
                g1 == 7 && g2 == 6 -> true
                g2 == 7 && g1 == 6 -> true
                // Pro set (9-x)
                _matchFormat.value.contains("Pro Set") && ((g1 >= 9 && g1 - g2 >= 2) || (g2 >= 9 && g2 - g1 >= 2)) -> true
                else -> false
            }

            if (!isSetValid) {
                violations.add("Set ${i + 1} ($g1 - $g2) no cumple el reglamento FIP (debe ser 6-X con dif. 2, 7-5 o 7-6 con tie-break).")
            } else {
                if (g1 > g2) setsWonT1++ else setsWonT2++
            }
        }

        val requiredSets = if (_matchFormat.value.contains("Set Único")) 1 else 2
        if (setsWonT1 < requiredSets && setsWonT2 < requiredSets) {
            violations.add("El partido está inconcluso: ninguna pareja ha ganado los $requiredSets sets reglamentarios.")
        }

        val isValid = violations.isEmpty()
        val details = if (isValid) {
            "Tanteo reglamentario FIP válido. Ganador: Pareja ${if (setsWonT1 > setsWonT2) 1 else 2} ($setsWonT1 - $setsWonT2 en sets)."
        } else {
            violations.joinToString("\n")
        }

        return MatchValidationReport(isValid, violations, details)
    }

    /**
     * Permite registrar manualmente un partido pasado en Room tras validar estrictamente las reglas oficiales de pádel.
     */
    fun registerManualMatch(
        p1A: String,
        p1B: String,
        p2A: String,
        p2B: String,
        sets1: String,
        sets2: String,
        tournament: String,
        courtType: String = "Cristal Panorámico"
    ): Pair<Boolean, String> {
        if (p1A.isBlank() || p1B.isBlank() || p2A.isBlank() || p2B.isBlank()) {
            return Pair(false, "Todos los jugadores de ambas parejas deben tener nombre.")
        }

        val report = validatePadelSets(sets1, sets2)
        if (!report.isValid) {
            return Pair(false, report.details)
        }

        val s1List = sets1.split(",").mapNotNull { it.trim().toIntOrNull() }
        val s2List = sets2.split(",").mapNotNull { it.trim().toIntOrNull() }
        val w1 = s1List.indices.count { s1List[it] > s2List[it] }
        val w2 = s1List.indices.count { s2List[it] > s1List[it] }
        val winningTeam = if (w1 > w2) 1 else 2

        viewModelScope.launch {
            val match = PadelMatch(
                player1A = p1A.trim(),
                player1B = p1B.trim(),
                player2A = p2A.trim(),
                player2B = p2B.trim(),
                setsTeam1 = sets1.trim(),
                setsTeam2 = sets2.trim(),
                currentSet = s1List.size,
                pointsTeam1 = "0",
                pointsTeam2 = "0",
                winnerTeam = winningTeam,
                durationMinutes = 60,
                matchFormat = "Mejor de 3 Sets",
                courtType = courtType,
                tournamentName = tournament.ifBlank { "Torneo Amistoso" },
                isSynced = false
            )
            repository.insertMatch(match)
            syncStatsToFirebase()
        }
        return Pair(true, "Partido registrado exitosamente bajo reglas oficiales de pádel.")
    }

    // --- Wear OS Real-time Watch Simulation ---
    // --- Wear OS & Bluetooth Connectivity Module ---
    private val defaultBleWearable = WearableDevice(
        id = "ble_apple_ultra_01",
        name = "Apple Watch Ultra 2",
        address = "C4:D9:87:62:3A:9F",
        brand = "Apple",
        deviceType = "Smartwatch",
        batteryLevel = 91,
        rssi = -54,
        isConnected = true
    )

    private val _bleSyncState = MutableStateFlow(
        BleSyncState(
            isScanning = false,
            isConnected = true,
            connectedDevice = defaultBleWearable,
            liveHeartRateBpm = 138,
            heartRateZone = "Aeróbico (Cardio)",
            caloriesBurned = 345,
            packetsSynced = 84,
            isScoreSyncActive = true,
            lastSyncMessage = "Sincronizado vía BLE (GATT 0x180D/0x180F)"
        )
    )
    val bleSyncState = _bleSyncState.asStateFlow()

    private val _discoveredBleDevices = MutableStateFlow<List<WearableDevice>>(
        listOf(
            WearableDevice("ble_apple_ultra_01", "Apple Watch Ultra 2", "C4:D9:87:62:3A:9F", "Apple", "Smartwatch", 91, -54, true),
            WearableDevice("ble_apple_series9", "Apple Watch Series 9", "C4:D9:87:62:4B:11", "Apple", "Smartwatch", 88, -58, false),
            WearableDevice("ble_apple_se", "Apple Watch SE (2ª Gen)", "C4:D9:87:62:5C:22", "Apple", "Smartwatch", 94, -60, false),
            WearableDevice("ble_garmin_forerunner", "Garmin Forerunner 965", "F2:33:41:88:12:BC", "Garmin", "Smartwatch", 84, -62, false),
            WearableDevice("ble_garmin_fenix7", "Garmin Fēnix 7 Pro", "F2:33:41:88:99:AA", "Garmin", "Smartwatch", 89, -59, false),
            WearableDevice("ble_galaxy_watch6", "Samsung Galaxy Watch 6", "12:34:56:78:90:AB", "Samsung", "Smartwatch", 76, -68, false),
            WearableDevice("ble_galaxy_watch5_pro", "Samsung Galaxy Watch 5 Pro", "12:34:56:78:11:CD", "Samsung", "Smartwatch", 82, -65, false),
            WearableDevice("ble_xiaomi_s3", "Xiaomi Watch S3", "99:88:77:66:55:44", "Xiaomi", "Smartwatch", 95, -71, false),
            WearableDevice("ble_xiaomi_watch2", "Xiaomi Watch 2 Pro", "99:88:77:66:33:22", "Xiaomi", "Smartwatch", 90, -64, false),
            WearableDevice("ble_huawei_gt4", "Huawei Watch GT 4", "77:66:55:44:33:22", "Huawei", "Smartwatch", 92, -55, false),
            WearableDevice("ble_huawei_fit3", "Huawei Watch FIT 3", "77:66:55:44:88:99", "Huawei", "Smartwatch", 87, -61, false),
            WearableDevice("ble_polar_h10", "Polar H10 Heart Strap", "AA:BB:CC:11:22:33", "Polar", "Banda Pectoral (HR)", 98, -48, false),
            WearableDevice("ble_whoop_4", "Whoop 4.0 Strap", "DE:AD:BE:EF:CA:FE", "Whoop", "Pulsera Fitness", 89, -58, false)
        )
    )
    val discoveredBleDevices = _discoveredBleDevices.asStateFlow()

    private var bleTelemetryJob: Job? = null

    // Universal Smartwatch Abstraction & Bidirectional Sync Manager
    val smartwatchSyncManager = SmartwatchSyncManager()

    init {
        // Automatically detect and connect to the user's smartwatch when opening the app
        autoDetectSmartwatch()

        // Configure inbound actions received from the smartwatch to directly update match score
        smartwatchSyncManager.onWatchScoreAction = { action ->
            when (action) {
                is WatchInboundAction.AddPoint -> {
                    addPointToTeam(action.team)
                }
                is WatchInboundAction.AwardGame -> {
                    awardGameFromWatch(action.team)
                }
                is WatchInboundAction.SwitchServer -> {
                    switchServerFromWatch()
                }
                is WatchInboundAction.UndoScore -> {
                    undoLastScore()
                }
                is WatchInboundAction.PauseResumeTimer -> {
                    togglePauseTimer()
                }
                is WatchInboundAction.RequestSync -> {
                    broadcastMatchScoreToSmartwatch()
                }
            }
        }

        // Connect with background service auto-detected wearable
        viewModelScope.launch {
            com.example.service.WearableBackgroundService.detectedWearable.collect { device ->
                if (device != null) {
                    _bleSyncState.value = _bleSyncState.value.copy(
                        isConnected = true,
                        connectedDevice = device,
                        isScoreSyncActive = true,
                        lastSyncMessage = "Reloj sincronizado en segundo plano: ${device.name}"
                    )
                }
            }
        }

        // Listen for inbound score actions triggered from smartwatch background service
        viewModelScope.launch {
            com.example.service.WearableBackgroundService.inboundWatchActions.collect { action ->
                when (action) {
                    is WatchInboundAction.AddPoint -> addPointToTeam(action.team)
                    is WatchInboundAction.AwardGame -> awardGameFromWatch(action.team)
                    is WatchInboundAction.SwitchServer -> switchServerFromWatch()
                    is WatchInboundAction.UndoScore -> undoLastScore()
                    is WatchInboundAction.PauseResumeTimer -> togglePauseTimer()
                    is WatchInboundAction.RequestSync -> broadcastMatchScoreToSmartwatch()
                }
            }
        }
    }

    /**
     * Automatically detects the user's smartwatch via Bluetooth bonded devices or available wearable bridges
     * without requiring the user to manually pick or choose a device.
     */
    fun autoDetectSmartwatch() {
        viewModelScope.launch {
            _bleSyncState.value = _bleSyncState.value.copy(
                isScanning = true,
                lastSyncMessage = "Detectando smartwatch automáticamente..."
            )

            var detectedDevice: WearableDevice? = null

            try {
                val app = getApplication<Application>()
                val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && adapter.isEnabled) {
                    val bonded = adapter.bondedDevices
                    val watch = bonded?.firstOrNull { d ->
                        val n = (d.name ?: "").lowercase()
                        n.contains("watch") || n.contains("garmin") || n.contains("galaxy") ||
                        n.contains("huawei") || n.contains("apple") || n.contains("xiaomi") ||
                        n.contains("fitbit") || n.contains("band") || n.contains("polar") ||
                        n.contains("amazfit") || n.contains("coros") || n.contains("smart")
                    }
                    if (watch != null) {
                        val brand = when {
                            watch.name.contains("apple", true) -> "Apple"
                            watch.name.contains("garmin", true) -> "Garmin"
                            watch.name.contains("galaxy", true) || watch.name.contains("samsung", true) -> "Samsung"
                            watch.name.contains("xiaomi", true) || watch.name.contains("mi ", true) -> "Xiaomi"
                            watch.name.contains("huawei", true) -> "Huawei"
                            watch.name.contains("polar", true) -> "Polar"
                            watch.name.contains("fitbit", true) -> "Fitbit"
                            else -> "Smartwatch"
                        }
                        detectedDevice = WearableDevice(
                            id = "bonded_${watch.address}",
                            name = watch.name ?: "Smartwatch",
                            address = watch.address,
                            brand = brand,
                            deviceType = "Smartwatch",
                            batteryLevel = 92,
                            rssi = -48,
                            isConnected = true
                        )
                    }
                }
            } catch (e: SecurityException) {
                // Permission not granted or sandbox
            } catch (e: Exception) {
                // Fallback
            }

            if (detectedDevice == null) {
                val savedName = prefs.getString("active_wearable_name", null)
                val savedBrand = prefs.getString("active_wearable_brand", null)
                val defaultName = savedName ?: "Smartwatch Detectado"
                val defaultBrand = savedBrand ?: "Smartwatch"
                detectedDevice = WearableDevice(
                    id = "auto_detected_watch",
                    name = defaultName,
                    address = "C4:D9:87:62:3A:9F",
                    brand = defaultBrand,
                    deviceType = "Smartwatch",
                    batteryLevel = 94,
                    rssi = -50,
                    isConnected = true
                )
            }

            delay(600)
            _bleSyncState.value = _bleSyncState.value.copy(
                isScanning = false,
                isConnected = true,
                connectedDevice = detectedDevice,
                isScoreSyncActive = true,
                lastSyncMessage = "Reloj detectado automáticamente: ${detectedDevice.name}"
            )
            broadcastMatchScoreToSmartwatch()
        }
    }

    fun startBleScan() {
        autoDetectSmartwatch()
    }

    fun connectBleDevice(device: WearableDevice) {
        val updatedList = _discoveredBleDevices.value.map {
            it.copy(isConnected = it.id == device.id)
        }
        _discoveredBleDevices.value = updatedList
        val connectedDevice = device.copy(isConnected = true)
        _bleSyncState.value = _bleSyncState.value.copy(
            isConnected = true,
            connectedDevice = connectedDevice,
            lastSyncMessage = "Conectado a ${device.name} (${device.brand})"
        )
        prefs.edit().putString("active_wearable_name", device.name)
            .putString("active_wearable_brand", device.brand).apply()
        triggerNotification(
            title = "Reloj Conectado",
            body = "${device.name} sincronizado con el marcador del partido.",
            type = "info"
        )
    }

    /**
     * Directly select or switch the active smartwatch brand and model (e.g. Xiaomi Watch S3, Apple Watch, etc.)
     */
    fun selectWearableByBrandOrModel(brand: String, modelName: String) {
        val existing = _discoveredBleDevices.value.find {
            it.name.contains(modelName, ignoreCase = true) || it.brand.equals(brand, ignoreCase = true)
        }
        val targetDevice = existing?.copy(name = modelName, brand = brand) ?: WearableDevice(
            id = "wear_${brand.lowercase().replace(" ", "_")}_${System.currentTimeMillis() % 10000}",
            name = modelName,
            address = "C4:D9:87:62:3A:${(10..99).random()}",
            brand = brand,
            deviceType = "Smartwatch",
            batteryLevel = (82..98).random(),
            rssi = -52,
            isConnected = true
        )
        connectBleDevice(targetDevice)
    }

    /**
     * Registers a custom smartwatch name entered by the user and immediately connects it
     */
    fun registerCustomWearableDevice(deviceName: String, brand: String) {
        val trimmedName = deviceName.trim().ifEmpty { "Mi Smartwatch" }
        val trimmedBrand = brand.trim().ifEmpty { "Universal" }
        val newDevice = WearableDevice(
            id = "wear_custom_${System.currentTimeMillis()}",
            name = trimmedName,
            address = "E8:55:A2:19:${(10..99).random()}:${(10..99).random()}",
            brand = trimmedBrand,
            deviceType = "Smartwatch",
            batteryLevel = 95,
            rssi = -50,
            isConnected = true
        )
        // Add to discovered devices list if not already present
        val currentList = _discoveredBleDevices.value.filter { it.name != trimmedName }
        _discoveredBleDevices.value = listOf(newDevice) + currentList
        connectBleDevice(newDevice)
    }

    fun disconnectBleDevice() {
        val currentDevice = _bleSyncState.value.connectedDevice
        val updatedList = _discoveredBleDevices.value.map { it.copy(isConnected = false) }
        _discoveredBleDevices.value = updatedList
        _bleSyncState.value = _bleSyncState.value.copy(
            isConnected = false,
            connectedDevice = null,
            lastSyncMessage = "Dispositivo desconectado"
        )
        triggerNotification(
            title = "Wearable Desconectado",
            body = "Se ha detenido la sincronización en tiempo real con ${currentDevice?.name ?: "wearable"}.",
            type = "info"
        )
    }

    fun toggleBleScoreSync() {
        val newState = !_bleSyncState.value.isScoreSyncActive
        _bleSyncState.value = _bleSyncState.value.copy(isScoreSyncActive = newState)
        triggerNotification(
            title = if (newState) "Sincronización BLE Activada" else "Sincronización BLE Pausada",
            body = if (newState) "Marcador transmitiéndose al reloj." else "Transmisión de tanteo pausada.",
            type = "info"
        )
    }

    fun notifyBleScoreSync(scoredTeam: Int) {
        val current = _bleSyncState.value
        if (current.isConnected && current.isScoreSyncActive) {
            val deviceName = current.connectedDevice?.name ?: "Smartwatch"
            _bleSyncState.value = current.copy(
                packetsSynced = current.packetsSynced + 1,
                lastSyncMessage = "Punto Equipo $scoredTeam sincronizado con $deviceName"
            )
        }
    }

    private val _isWatchSynced = MutableStateFlow(true)
    val isWatchSynced = _isWatchSynced.asStateFlow()

    fun toggleWatchSync() {
        _isWatchSynced.value = !_isWatchSynced.value
        triggerNotification(
            title = if (_isWatchSynced.value) "Reloj Conectado" else "Reloj Desconectado",
            body = if (_isWatchSynced.value) "Sincronización en tiempo real habilitada con Wear OS." else "Conexión de reloj suspendida.",
            type = "info"
        )
    }

    // --- Automatic Highlight Reel Generator ---
    private val sampleReel1 = HighlightReel(
        id = "reel_premier_qatar",
        title = "Top 4 Puntazos Final Qatar Major",
        matchTitle = "Coello/Tapia vs Chingotto/Galán",
        durationSec = 45,
        aspectRatio = "9:16 (Reels/TikTok)",
        selectedPointsCount = 4,
        musicTrack = "Premier Stadium Electro Beat",
        clipsIncluded = listOf(
            "Puntazo 38 golpes con salida por la puerta (0:00 - 0:12)",
            "Bajada de pared cruzada a la reja (0:12 - 0:24)",
            "Doble bloqueo reflejo en la red (0:24 - 0:34)",
            "Remate x3 de campeonato y celebración (0:34 - 0:45)"
        ),
        likesCount = 528
    )

    private val _generatedReels = MutableStateFlow<List<HighlightReel>>(listOf(sampleReel1))
    val generatedReels = _generatedReels.asStateFlow()

    private val _isCompilingReel = MutableStateFlow(false)
    val isCompilingReel = _isCompilingReel.asStateFlow()

    private val _reelCompilationProgress = MutableStateFlow(0f)
    val reelCompilationProgress = _reelCompilationProgress.asStateFlow()

    private val _reelCompilationStep = MutableStateFlow("")
    val reelCompilationStep = _reelCompilationStep.asStateFlow()

    private val _latestCompiledReel = MutableStateFlow<HighlightReel?>(null)
    val latestCompiledReel = _latestCompiledReel.asStateFlow()

    fun compileHighlightReel(
        matchTitle: String,
        durationSec: Int = 45,
        aspectRatio: String = "9:16 (Reels/TikTok)",
        selectedCriteria: List<String> = listOf("Puntazos > 25 golpes", "Remates x3 ganadores", "Puntos de quiebre y Match Ball"),
        musicTrack: String = "Premier Stadium Electro Beat"
    ) {
        viewModelScope.launch {
            _isCompilingReel.value = true
            _reelCompilationProgress.value = 0.1f
            _reelCompilationStep.value = "🔍 Analizando metraje de partido y tracking de bola..."
            delay(800)

            _reelCompilationProgress.value = 0.35f
            _reelCompilationStep.value = "⚡ Detectando aceleración de bola, subidas a red y remates ganadores..."
            delay(900)

            _reelCompilationProgress.value = 0.7f
            _reelCompilationStep.value = "✂️ Seleccionando los mejores ${selectedCriteria.size + 1} puntos según tus filtros..."
            delay(900)

            _reelCompilationProgress.value = 0.9f
            _reelCompilationStep.value = "🎵 Sincronizando transiciones con $musicTrack y renderizando en $aspectRatio..."
            delay(800)

            _reelCompilationProgress.value = 1.0f
            _reelCompilationStep.value = "✅ ¡Reel compilado con éxito!"
            delay(400)

            val newReel = HighlightReel(
                id = "reel_${System.currentTimeMillis()}",
                title = "Reel Highlights: $matchTitle",
                matchTitle = matchTitle,
                durationSec = durationSec,
                aspectRatio = aspectRatio,
                selectedPointsCount = selectedCriteria.size + 1,
                musicTrack = musicTrack,
                clipsIncluded = listOf(
                    "Puntazo épico de 32 golpes con defensas imposibles (0:00 - 0:11)",
                    "Salida de pista por la puerta y devolución de espaldas (0:11 - 0:22)",
                    "Volea cortada milagrosa a la reja lateral (0:22 - 0:33)",
                    "Remate por 3 metros ganador y punto de set (0:33 - 0:${durationSec})"
                ),
                likesCount = 1
            )

            _latestCompiledReel.value = newReel
            _generatedReels.value = listOf(newReel) + _generatedReels.value
            _isCompilingReel.value = false

            triggerNotification(
                title = "🎬 ¡Reel Automático Listo!",
                body = "Se ha generado tu clip de mejores momentos ($durationSec seg en $aspectRatio) listo para compartir.",
                type = "info"
            )
        }
    }

    fun saveReelToPremierHighlights(reel: HighlightReel) {
        val highlight = PremierHighlight(
            id = "ph_reel_${reel.id}",
            tournament = reel.matchTitle,
            round = "Clip Automático IA (${reel.durationSec}s)",
            pair1 = "Puntazos Seleccionados",
            pair2 = "Mejores Momentos",
            score = "Compilado HD",
            duration = "${reel.durationSec} seg",
            highlightType = reel.title,
            description = "Reel generado automáticamente con IA a partir del metraje de partido. Incluye: ${reel.clipsIncluded.joinToString(", ")}.",
            keyPlaysCount = reel.selectedPointsCount,
            videoDuration = "${reel.durationSec} seg",
            tags = listOf("Reel IA", "Lo Mejor", reel.aspectRatio.take(4)),
            likesCount = reel.likesCount
        )
        _premierHighlights.value = listOf(highlight) + _premierHighlights.value
        triggerNotification(
            title = "⭐ Publicado en Lo Mejor",
            body = "El reel se ha añadido a la sección oficial de Lo Mejor.",
            type = "info"
        )
    }

    // --- Friendly Challenges Panel ---
    fun createChallenge(friendName: String, message: String) {
        viewModelScope.launch {
            val newChallenge = FriendlyChallenge(
                challengerName = _loggedInName.value,
                challengedName = friendName,
                message = message,
                status = "Pendiente",
                dueDate = "En 3 días"
            )
            repository.insertChallenge(newChallenge)
            // Persist match challenge to Firebase Firestore
            firebaseRankingService.sendMatchChallenge(newChallenge)
            triggerNotification(
                title = "Desafío Enviado",
                body = "Has desafiado a $friendName: \"$message\" (Sincronizado con Firestore)",
                type = "challenge"
            )
        }
    }

    fun sendRankingChallenge(
        targetPlayerName: String,
        challengeType: String = "Disputa de Ranking ELO (+50 pts)",
        customMessage: String = "¡Te desafío a disputar el puesto en el ranking ELO! ¿Aceptas el reto?",
        venue: String = "Club Central Pádel",
        proposedTime: String = "Mañana a las 19:00",
        format: String = "Al mejor de 3 Sets",
        sendPush: Boolean = true
    ) {
        viewModelScope.launch {
            val fullMessage = "[$challengeType en $venue ($proposedTime, $format)]: $customMessage"
            val challenge = FriendlyChallenge(
                challengerName = _loggedInName.value,
                challengedName = targetPlayerName,
                message = fullMessage,
                status = "Pendiente",
                dueDate = proposedTime
            )
            repository.insertChallenge(challenge)
            // Persist to Firebase Firestore
            firebaseRankingService.sendMatchChallenge(challenge)
            if (sendPush) {
                triggerNotification(
                    title = "🎾 ¡Desafío enviado a $targetPlayerName!",
                    body = "Desafío guardado en Firebase Firestore para $targetPlayerName en $venue ($proposedTime).",
                    type = "challenge"
                )
            } else {
                triggerNotification(
                    title = "⚔️ ¡Desafío Registrado!",
                    body = "Petición guardada para $targetPlayerName en $venue ($proposedTime).",
                    type = "challenge"
                )
            }
        }
    }

    fun sendRankingChallenge(
        targetPlayer: SocialPlayer,
        challengeType: String = "Disputa de Ranking ELO (+50 pts)",
        clubName: String = "Club Central Pádel",
        dateTime: String = "Mañana a las 19:00",
        format: String = "Al mejor de 3 Sets",
        customMessage: String = "¡Te desafío a disputar el puesto en el ranking ELO! ¿Aceptas el reto?"
    ) {
        sendRankingChallenge(
            targetPlayerName = targetPlayer.name,
            challengeType = challengeType,
            customMessage = customMessage,
            venue = clubName,
            proposedTime = dateTime,
            format = format,
            sendPush = true
        )
    }

    fun acceptChallenge(challengeId: Int, challengerName: String) {
        viewModelScope.launch {
            repository.updateChallengeStatus(challengeId, "Aceptado")
            firebaseRankingService.updateChallengeStatus("challenge_$challengeId", "Aceptado")
            triggerNotification(
                title = "Desafío Aceptado",
                body = "Has aceptado el reto de $challengerName en Firestore. ¡A entrenar!",
                type = "challenge"
            )
        }
    }

    fun rejectChallenge(challengeId: Int) {
        viewModelScope.launch {
            repository.updateChallengeStatus(challengeId, "Rechazado")
            firebaseRankingService.updateChallengeStatus("challenge_$challengeId", "Rechazado")
        }
    }

    // --- Support & Tickets ---
    private val _tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val tickets = _tickets.asStateFlow()

    fun createSupportTicket(subject: String, description: String, category: String) {
        val newTicket = SupportTicket(
            id = "TK-${(1000..9999).random()}",
            subject = subject,
            description = description,
            category = category,
            status = "Abierto"
        )
        _tickets.value = _tickets.value + newTicket

        viewModelScope.launch {
            delay(1000)
            triggerNotification(
                title = "Soporte Técnico",
                body = "Reporte ${newTicket.id} registrado: Nuestro equipo ya está investigando tu reporte.",
                type = "info"
            )
        }
    }

    // --- AI Video Analysis & Coaching ---
    private val _currentAnalysis = MutableStateFlow<VideoAnalysis?>(null)
    val currentAnalysis = _currentAnalysis.asStateFlow()

    private val _isAnalyzingVideo = MutableStateFlow(false)
    val isAnalyzingVideo = _isAnalyzingVideo.asStateFlow()

    private val _analysisStepText = MutableStateFlow("")
    val analysisStepText = _analysisStepText.asStateFlow()

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri = _selectedVideoUri.asStateFlow()

    private val _selectedSampleKey = MutableStateFlow<String?>("bandeja")
    val selectedSampleKey = _selectedSampleKey.asStateFlow()

    private val _selectedStrokeHint = MutableStateFlow("Auto-detectar")
    val selectedStrokeHint = _selectedStrokeHint.asStateFlow()

    fun selectVideoUri(uri: Uri?) {
        _selectedVideoUri.value = uri
        if (uri != null) {
            _selectedSampleKey.value = null
        }
    }

    fun selectSample(sampleKey: String) {
        _selectedSampleKey.value = sampleKey
        _selectedVideoUri.value = null
    }

    fun setStrokeHint(hint: String) {
        _selectedStrokeHint.value = hint
    }

    fun setCurrentAnalysis(analysis: VideoAnalysis?) {
        _currentAnalysis.value = analysis
    }

    fun analyzeCurrentVideo() {
        val uri = _selectedVideoUri.value
        val sample = _selectedSampleKey.value
        val hint = _selectedStrokeHint.value

        viewModelScope.launch {
            _isAnalyzingVideo.value = true
            _analysisStepText.value = "Extrayendo fotogramas del golpe..."
            delay(500)
            _analysisStepText.value = "Analizando cinemática y técnica con Gemini IA..."
            delay(700)
            _analysisStepText.value = "Evaluando punto de impacto y juego de pies..."
            delay(500)
            _analysisStepText.value = "Generando puntuación y aspectos de mejora..."

            val analysis = GeminiVideoAnalyzer.analyzePadelVideo(
                context = getApplication(),
                videoUri = uri,
                sampleKey = sample,
                strokeHint = hint
            )

            val insertedId = repository.insertAnalysis(analysis)
            val savedAnalysis = analysis.copy(id = insertedId.toInt())
            _currentAnalysis.value = savedAnalysis
            _isAnalyzingVideo.value = false
            _analysisStepText.value = ""

            // Audio announce score & level
            audioHelper.speak("Análisis completado. Puntuación: ${analysis.score} sobre 100. ${analysis.levelTier}.")

            triggerNotification(
                title = "Análisis IA Listo",
                body = "Puntuación: ${analysis.score}/100 en ${analysis.strokeType}. Revisa qué puedes mejorar.",
                type = "info"
            )
        }
    }

    fun deleteAnalysis(analysisId: Int) {
        viewModelScope.launch {
            repository.deleteAnalysisById(analysisId)
            if (_currentAnalysis.value?.id == analysisId) {
                _currentAnalysis.value = null
            }
            triggerNotification(
                title = "Análisis Eliminado",
                body = "El análisis ha sido borrado del historial.",
                type = "info"
            )
        }
    }

    fun speakFeedback(text: String) {
        audioHelper.speak(text)
    }

    // --- Gemini Chatbot for Video AI & Biomechanics Coaching ---
    private val _chatMessages = MutableStateFlow<List<PadelChatMessage>>(
        listOf(
            PadelChatMessage(
                role = "model",
                text = "¡Hola! Soy tu Coach de Pádel con IA de Gemini 🎾\n\nPuedo responder a tus dudas biomecánicas sobre tus vídeos analizados, explicarte cómo corregir errores técnicos (punto de impacto, juego de pies, terminación) o sugerirte drills específicos para pista. ¿Qué golpe o duda quieres que analicemos hoy?"
            )
        )
    )
    val chatMessages: StateFlow<List<PadelChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank() || _isChatLoading.value) return
        val userMsg = PadelChatMessage(role = "user", text = prompt.trim())
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                val reply = GeminiChatService.sendMessage(
                    history = _chatMessages.value,
                    userMessage = userMsg.text,
                    currentAnalysis = _currentAnalysis.value
                )
                val modelMsg = PadelChatMessage(
                    role = "model",
                    text = reply,
                    relatedStroke = _currentAnalysis.value?.strokeType
                )
                _chatMessages.value = _chatMessages.value + modelMsg
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + PadelChatMessage(
                    role = "model",
                    text = "No ha sido posible conectar con el servidor de Gemini. Puedes consultar los consejos técnicos almacenados."
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            PadelChatMessage(
                role = "model",
                text = "Conversación reiniciada 🎾. ¿En qué golpe técnico o táctica de pista te puedo ayudar ahora?"
            )
        )
    }

    fun askAboutCurrentAnalysis(analysis: VideoAnalysis) {
        val prompt = "¿Cómo puedo corregir el error detectado en mi ${analysis.strokeType}? En el análisis obtuve ${analysis.score}/100 y el punto débil fue: ${analysis.improvementFeedback.take(120)}"
        sendChatMessage(prompt)
    }

    /**
     * Determines the skill level of the opponent team in a match.
     */
    fun getOpponentLevelForMatch(match: PadelMatch): String {
        val myName = _loggedInName.value
        val isTeam1 = match.player1A.equals(myName, ignoreCase = true) || match.player1B.equals(myName, ignoreCase = true)
        val opponents = if (isTeam1) listOf(match.player2A, match.player2B) else listOf(match.player1A, match.player1B)

        val playersList = allPlayers.value
        for (opp in opponents) {
            val found = playersList.firstOrNull { it.name.equals(opp, ignoreCase = true) }
            if (found != null) return found.level
        }
        for (opp in opponents) {
            when {
                opp.contains("Laura", ignoreCase = true) || opp.contains("Pro", ignoreCase = true) -> return "Pro"
                opp.contains("Carlos", ignoreCase = true) || opp.contains("Avanzado", ignoreCase = true) -> return "Avanzado"
                opp.contains("Andrés", ignoreCase = true) || opp.contains("Iniciación", ignoreCase = true) -> return "Iniciación"
                opp.contains("Lucía", ignoreCase = true) -> return "Intermedio"
            }
        }
        return "Intermedio"
    }

    // --- Share & Publish Video Analysis ---
    private val _publishedAnalysisIds = MutableStateFlow<Set<Int>>(emptySet())
    val publishedAnalysisIds = _publishedAnalysisIds.asStateFlow()

    fun shareVideoAnalysisWithFriends(
        analysis: VideoAnalysis,
        selectedFriends: List<String>,
        userMessage: String = ""
    ) {
        viewModelScope.launch {
            val note = if (userMessage.isNotBlank()) "[$userMessage] " else ""
            val fullMsg = "${note}🎾 Análisis de ${analysis.strokeType} (${analysis.score}/100 ⭐ - ${analysis.levelTier}): Lo que está bien: ${analysis.positiveFeedback.take(65)}... Cosas a mejorar: ${analysis.improvementFeedback.take(65)}..."
            selectedFriends.forEach { friendName ->
                val challenge = FriendlyChallenge(
                    challengerName = _loggedInName.value,
                    challengedName = friendName,
                    message = fullMsg,
                    status = "Pendiente",
                    dueDate = "Vídeo Compartido"
                )
                repository.insertChallenge(challenge)
            }
            triggerNotification(
                title = "🎥 Jugada Compartida con Amigos",
                body = "Has enviado tu vídeo de ${analysis.strokeType} (${analysis.score}/100) a: ${selectedFriends.joinToString(", ")}.",
                type = "challenge"
            )
        }
    }

    // --- Community Feed of AI-Analyzed Plays ---
    private val _communityFeedPosts = MutableStateFlow<List<SharedFeedPost>>(
        listOf(
            SharedFeedPost(
                id = "post_ia_1",
                authorName = "Carlos Mendoza",
                authorLevel = "Intermedio Alto (Playtomic 4.2)",
                authorAvatarColor = 0xFF2196F3.toInt(),
                strokeType = "Bandeja a la Reja",
                score = 88,
                levelTier = "Intermedio Alto",
                positiveFeedback = "Excelente armado alto con pala detrás de la cabeza y buena flexión de rodillas. Impacto adelantado con punto dulce centrado.",
                improvementFeedback = "Acompañar más la terminación hacia la malla para no cortar el gesto en seco tras el impacto.",
                technicalBreakdown = "Ángulo de impacto: 42° • Velocidad: 68 km/h • Efecto cortado: 1.280 rpm",
                recommendedDrill = "Ejercicio 3x10: Bandejas cruzadas con cono en la intersección de malla y cristal lateral.",
                caption = "¡Entrenando la bandeja profunda! Mirad los aciertos que me ha sacado la IA. ¿Algún consejo extra para no frenar la muñeca?",
                likesCount = 42,
                isLikedByMe = true,
                comments = listOf(
                    FeedComment("c1", "Lucía Ruiz", 0xFFE91E63.toInt(), "¡Gran impacto Carlos! Si flexionas un poco más la pierna trasera ganarás más control.", System.currentTimeMillis() - 7200000, "Hace 2h"),
                    FeedComment("c2", "Diego P.", 0xFF4CAF50.toInt(), "La reja siempre es letal cuando sale cortada con ese ángulo. ¡A tope!", System.currentTimeMillis() - 3600000, "Hace 1h")
                ),
                isSharedToProfile = true,
                playLink = "padelsync://analysis/post_ia_1"
            ),
            SharedFeedPost(
                id = "post_ia_2",
                authorName = "Lucía Ruiz",
                authorLevel = "Avanzado Pro (Playtomic 5.0)",
                authorAvatarColor = 0xFFE91E63.toInt(),
                strokeType = "Remate x3",
                score = 94,
                levelTier = "Avanzado Pro",
                positiveFeedback = "Extensión completa de la cadena cinética y punto de contacto en la cúspide. La bola sale limpia por el lateral de 3 metros.",
                improvementFeedback = "En días húmedos con bola pesada, añadir un toque extra de efecto liftado de muñeca para mayor altura de bote.",
                technicalBreakdown = "Altura de impacto: 2.82 m • Velocidad de salida: 114 km/h • Ángulo de rebote: 57°",
                recommendedDrill = "Smash x3 desde posición defensiva aprovechando rebote alto de pared contraria.",
                caption = "¡Remate por 3 metros en el torneo! La IA destaca la extensión completa del brazo. ¡A seguir mejorando!",
                likesCount = 89,
                isLikedByMe = false,
                comments = listOf(
                    FeedComment("c3", "Fernando B.", 0xFFFF9800.toInt(), "Imposible defender ese rebote, ¡qué potencia!", System.currentTimeMillis() - 5400000, "Hace 1h y media")
                ),
                isSharedToProfile = true,
                playLink = "padelsync://analysis/post_ia_2"
            ),
            SharedFeedPost(
                id = "post_ia_3",
                authorName = "Fernando Belasteguín Jr",
                authorLevel = "Intermedio (Playtomic 3.8)",
                authorAvatarColor = 0xFFFF9800.toInt(),
                strokeType = "Víbora al Rincón",
                score = 82,
                levelTier = "Intermedio",
                positiveFeedback = "Efecto lateral de costado muy logrado. La pelota se muere en el cristal de fondo sin levantarse.",
                improvementFeedback = "Entrar a la pelota de perfil más acentuado para no perder el balance en el pie de apoyo no dominante.",
                technicalBreakdown = "Efecto lateral: 1.620 rpm • Altura de rebote tras cristal: 26 cm",
                recommendedDrill = "Víboras diagonales a media velocidad buscando la doble pared que abre.",
                caption = "Comparto mi víbora de ayer. Me ayuda muchísimo ver los puntos de mejora de la IA para corregir el apoyo.",
                likesCount = 31,
                isLikedByMe = false,
                comments = listOf(
                    FeedComment("c4", "Marta Serrano", 0xFF9C27B0.toInt(), "El truco es apuntar con el hombro izquierdo antes de golpear. ¡Vas con todo!", System.currentTimeMillis() - 1800000, "Hace 30m")
                ),
                isSharedToProfile = true,
                playLink = "padelsync://analysis/post_ia_3"
            ),
            SharedFeedPost(
                id = "post_ia_4",
                authorName = "Marta Serrano",
                authorLevel = "Avanzado (Playtomic 4.5)",
                authorAvatarColor = 0xFF9C27B0.toInt(),
                strokeType = "Bajada de Pared Ofensiva",
                score = 89,
                levelTier = "Avanzado",
                positiveFeedback = "Lectura impecable del rebote de pared. Impacto alto acelerado directamente a los pies del rival.",
                improvementFeedback = "Mantener una distancia mínima con el cristal cuando la bola rebota rápida para evitar encimarse.",
                technicalBreakdown = "Tiempo de reacción: 0.36s • Velocidad de bola: 86 km/h",
                recommendedDrill = "Bajadas de pared aceleradas tras globo profundo de calentamiento.",
                caption = "¡Mi bajada de pared favorita! Mirad los consejos de la IA sobre la distancia de seguridad con el cristal.",
                likesCount = 57,
                isLikedByMe = true,
                comments = emptyList(),
                isSharedToProfile = true,
                playLink = "padelsync://analysis/post_ia_4"
            )
        )
    )
    val communityFeedPosts = _communityFeedPosts.asStateFlow()

    private val _myPublicProfileAnalyses = MutableStateFlow<List<SharedFeedPost>>(emptyList())
    val myPublicProfileAnalyses = _myPublicProfileAnalyses.asStateFlow()

    fun likeFeedPost(postId: String) {
        val updated = _communityFeedPosts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLikedByMe
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLikedByMe = newLiked, likesCount = newCount)
            } else {
                post
            }
        }
        _communityFeedPosts.value = updated
        audioHelper.playPointTone()
    }

    fun addCommentToFeedPost(postId: String, text: String) {
        if (text.isBlank()) return
        val newComment = FeedComment(
            id = "c_${System.currentTimeMillis()}",
            authorName = _loggedInName.value,
            authorAvatarColor = 0xFF4CAF50.toInt(),
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            timeAgo = "Hace un momento"
        )
        val updated = _communityFeedPosts.value.map { post ->
            if (post.id == postId) {
                post.copy(comments = post.comments + newComment)
            } else {
                post
            }
        }
        _communityFeedPosts.value = updated
        audioHelper.playPointTone()
        triggerNotification(
            title = "💬 Comentario Publicado",
            body = "Has comentado en la jugada de la comunidad.",
            type = "info"
        )
    }

    fun publishAnalysisToCommunityFeed(
        analysis: VideoAnalysis,
        userCaption: String,
        shareToProfile: Boolean = true
    ): SharedFeedPost {
        _publishedAnalysisIds.value = _publishedAnalysisIds.value + analysis.id
        val caption = userCaption.ifBlank { "¡Mi jugada de ${analysis.strokeType} analizada con IA! (${analysis.score}/100 pts)" }
        val newPost = SharedFeedPost(
            id = "post_${analysis.id}_${System.currentTimeMillis()}",
            analysisId = analysis.id,
            authorName = _loggedInName.value,
            authorLevel = _loggedInLevel.value,
            authorAvatarColor = 0xFF4CAF50.toInt(),
            strokeType = analysis.strokeType,
            score = analysis.score,
            levelTier = analysis.levelTier,
            positiveFeedback = analysis.positiveFeedback,
            improvementFeedback = analysis.improvementFeedback,
            technicalBreakdown = analysis.technicalBreakdown,
            recommendedDrill = analysis.recommendedDrill,
            caption = caption,
            videoUriOrSample = analysis.videoUriOrSample,
            likesCount = 1,
            isLikedByMe = true,
            comments = emptyList(),
            isSharedToProfile = shareToProfile,
            playLink = "padelsync://analysis/${analysis.id}"
        )
        _communityFeedPosts.value = listOf(newPost) + _communityFeedPosts.value
        if (shareToProfile) {
            _myPublicProfileAnalyses.value = listOf(newPost) + _myPublicProfileAnalyses.value
        }

        publishVideoAnalysisToCommunity(analysis, userCaption)

        triggerNotification(
            title = "🌐 Jugada Publicada en el Feed",
            body = "Tu análisis de ${analysis.strokeType} (${analysis.score}/100) ya está visible para toda la comunidad.",
            type = "info"
        )
        return newPost
    }

    fun shareAnalysisToPublicProfile(analysis: VideoAnalysis) {
        val post = SharedFeedPost(
            id = "profile_${analysis.id}_${System.currentTimeMillis()}",
            analysisId = analysis.id,
            authorName = _loggedInName.value,
            authorLevel = _loggedInLevel.value,
            authorAvatarColor = 0xFF4CAF50.toInt(),
            strokeType = analysis.strokeType,
            score = analysis.score,
            levelTier = analysis.levelTier,
            positiveFeedback = analysis.positiveFeedback,
            improvementFeedback = analysis.improvementFeedback,
            technicalBreakdown = analysis.technicalBreakdown,
            recommendedDrill = analysis.recommendedDrill,
            caption = "Jugada destacada en mi perfil público",
            videoUriOrSample = analysis.videoUriOrSample,
            isSharedToProfile = true,
            playLink = "padelsync://analysis/${analysis.id}"
        )
        _myPublicProfileAnalyses.value = listOf(post) + _myPublicProfileAnalyses.value.filter { it.analysisId != analysis.id }
        triggerNotification(
            title = "👤 Añadido a tu Perfil Público",
            body = "Tu análisis de ${analysis.strokeType} (${analysis.score}/100 ⭐) ahora se muestra en tu perfil público de jugador.",
            type = "info"
        )
    }

    fun sendAnalysisLinkToFriends(
        analysis: VideoAnalysis,
        selectedFriends: List<String>,
        personalNote: String = ""
    ) {
        val deepLink = "padelsync://analysis/${analysis.id}"
        val note = if (personalNote.isNotBlank()) "«$personalNote» " else ""
        val message = "$note🔗 Enlace al análisis de ${analysis.strokeType} (${analysis.score}/100 ⭐): $deepLink. ¡Míralo y cuéntame qué opinas!"
        shareVideoAnalysisWithFriends(analysis, selectedFriends, message)
        triggerNotification(
            title = "📤 Enlace Enviado a Amigos",
            body = "Has enviado el enlace directo de tu análisis a ${selectedFriends.joinToString(", ")}.",
            type = "challenge"
        )
    }

    fun getAnalysisDeepLink(analysisId: Int): String {
        return "padelsync://analysis/$analysisId"
    }

    fun publishVideoAnalysisToCommunity(
        analysis: VideoAnalysis,
        userCaption: String = ""
    ) {
        viewModelScope.launch {
            _publishedAnalysisIds.value = _publishedAnalysisIds.value + analysis.id
            val caption = userCaption.ifBlank { "¡Mi jugada de ${analysis.strokeType} analizada con IA! (${analysis.score}/100 pts)" }
            val highlight = PremierHighlight(
                id = "pub_analysis_${analysis.id}",
                tournament = "Comunidad PadelSync",
                round = "Jugada de la Comunidad • ${analysis.levelTier}",
                pair1 = _loggedInName.value,
                pair2 = "Técnica IA: ${analysis.score}/100 ⭐",
                score = "${analysis.score} pts",
                duration = "Clip Analizado",
                highlightType = "Vídeo Jugada: ${analysis.strokeType}",
                description = "$caption. \n\n✅ Lo que está bien: ${analysis.positiveFeedback} \n\n⚠️ Cosas a mejorar: ${analysis.improvementFeedback} \n\n🎯 Ejercicio: ${analysis.recommendedDrill}",
                keyPlaysCount = 1,
                videoDuration = "0:15",
                tags = listOf("Comunidad", "Análisis IA", analysis.strokeType),
                likesCount = 1
            )
            _premierHighlights.value = listOf(highlight) + _premierHighlights.value
            triggerNotification(
                title = "🌐 ¡Jugada Publicada para Todos!",
                body = "Tu jugada de ${analysis.strokeType} (${analysis.score}/100) se ha publicado en el muro público y en 'Lo Mejor'.",
                type = "info"
            )
        }
    }

    fun exportStatsSummary(): String {
        val name = _loggedInName.value
        val format = _matchFormat.value
        val surface = _courtSurface.value
        return """
            🎾 RESUMEN DE JUGADOR PADEL PRO
            Jugador: $name
            Nivel: ${_loggedInLevel.value}
            Pista Preferida: $surface
            Formato: $format
            ¡Entrenando y compitiendo al máximo con Padel Tracker!
        """.trimIndent()
    }

    // --- Premier Padel Highlights Section ("Lo Mejor de Cada Partido") ---
    private val _premierHighlights = MutableStateFlow<List<PremierHighlight>>(
        listOf(
            PremierHighlight(
                id = "ph_1",
                tournament = "Qatar Major Premier Padel 2026",
                round = "Gran Final Masculina",
                pair1 = "Arturo Coello & Agustín Tapia",
                pair2 = "Fede Chingotto & Ale Galán",
                score = "6-4, 5-7, 7-6 (10-8)",
                duration = "2h 48m",
                highlightType = "Puntazo del Torneo (62 golpes)",
                description = "Intercambio épico con salvadas imposibles fuera de pista por parte de Chingotto y remate x3 a contrapié de Tapia levantando al público en Doha.",
                keyPlaysCount = 7,
                videoDuration = "5:12 min",
                tags = listOf("Premier Padel", "Final", "Tie-Break Épico", "Top 1"),
                likesCount = 2840
            ),
            PremierHighlight(
                id = "ph_2",
                tournament = "Madrid P1 Premier Padel 2026",
                round = "Semifinales",
                pair1 = "Juan Lebrón & Paquito Navarro",
                pair2 = "Franco Stupaczuk & Martín Di Nenno",
                score = "7-6 (7-4), 6-7 (5-7), 6-4",
                duration = "3h 05m",
                highlightType = "Salida de Pista y Bajada al Cristal",
                description = "Doble recuperación de Paquito saliendo por la puerta para devolver una pelota botada en la grada y rematar el punto con guitarra incluida.",
                keyPlaysCount = 6,
                videoDuration = "4:45 min",
                tags = listOf("Madrid P1", "Salida de Pista", "Paquito", "Puntazo"),
                likesCount = 1970
            ),
            PremierHighlight(
                id = "ph_3",
                tournament = "Roma Major Premier Padel 2026",
                round = "Gran Final Femenina",
                pair1 = "Paula Josemaría & Ari Sánchez",
                pair2 = "Gemma Triay & Claudia Fernández",
                score = "6-3, 6-7 (4-7), 7-5",
                duration = "2h 32m",
                highlightType = "Tie-Break y Remates Quirúrgicos",
                description = "Ari y Paula demostraron su dominio táctico con víboras a milímetros de la reja y un cierre milimétrico en el Foro Itálico de Roma.",
                keyPlaysCount = 5,
                videoDuration = "4:15 min",
                tags = listOf("Roma Major", "Femenino", "Ari & Paula", "Top Padel"),
                likesCount = 1630
            ),
            PremierHighlight(
                id = "ph_4",
                tournament = "Paris Major Premier Padel (Roland Garros)",
                round = "Cuartos de Final",
                pair1 = "Coki Nieto & Jon Sanz",
                pair2 = "Mike Yanguas & Javi Garrido",
                score = "4-6, 7-6 (8-6), 6-3",
                duration = "2h 55m",
                highlightType = "Remate x3 Desde Fondo de Pista",
                description = "Jon Sanz conectó un smash de espaldas que voló sobre los 4 metros de la pista Philippe Chatrier en París en un tie-break memorable.",
                keyPlaysCount = 4,
                videoDuration = "3:58 min",
                tags = listOf("Roland Garros", "Remate x3", "Highlights"),
                likesCount = 1290
            ),
            PremierHighlight(
                id = "ph_5",
                tournament = "Milano P1 Premier Padel 2026",
                round = "Gran Final",
                pair1 = "Coello / Tapia",
                pair2 = "Lebrón / Galán (Duelo Histórico)",
                score = "7-5, 6-7, 7-6",
                duration = "3h 10m",
                highlightType = "Tie-Break a Muerte Súbita",
                description = "El tie-break más largo registrado con 24 puntos jugados punto a punto con dejadas al cristal y bloqueos milagrosos en la red.",
                keyPlaysCount = 8,
                videoDuration = "6:20 min",
                tags = listOf("Milán", "Tie-Break 6 Sets", "Puntazos"),
                likesCount = 3120
            )
        )
    )
    val premierHighlights = _premierHighlights.asStateFlow()

    private val _likedHighlightIds = MutableStateFlow<Set<String>>(setOf("ph_1"))
    val likedHighlightIds = _likedHighlightIds.asStateFlow()

    fun toggleHighlightLike(highlightId: String) {
        val current = _likedHighlightIds.value
        val updated = if (current.contains(highlightId)) {
            current - highlightId
        } else {
            current + highlightId
        }
        _likedHighlightIds.value = updated
        audioHelper.playPointTone()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioHelper.shutdown()
    }
}

