package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "padel_matches")
data class PadelMatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val player1A: String,
    val player1B: String,
    val player2A: String,
    val player2B: String,
    val setsTeam1: String, // Comma-separated scores, e.g. "6,4,6"
    val setsTeam2: String, // Comma-separated scores, e.g. "4,6,3"
    val currentSet: Int = 1,
    val pointsTeam1: String = "0", // "0", "15", "30", "40", "Ad"
    val pointsTeam2: String = "0",
    val winnerTeam: Int = 0, // 0 = ongoing, 1 = Team 1, 2 = Team 2
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isFriendlyChallenge: Boolean = false,
    val challengeId: Int? = null,
    val durationMinutes: Int = 45,
    val matchFormat: String = "3 Sets",
    val courtType: String = "Azul Premier",
    val tournamentName: String = "Liga Premier Padel",
    val matchResultType: String = "Torneo", // "Torneo", "Liga", "Amistoso", "Reto ELO"
    val scheduledDate: String = "",
    val scheduledStartTime: String = "",
    val scheduledEndTime: String = "",
    val player1BStatus: String = "Aceptado", // "Pendiente", "Aceptado", "Rechazado"
    val player2AStatus: String = "Aceptado",
    val player2BStatus: String = "Aceptado",
    val isScoreConfirmed: Boolean = false,
    val confirmedByPlayers: String = "" // Comma-separated list of player names who confirmed the score
)

data class FriendRequest(
    val id: String = "",
    val senderName: String = "",
    val senderLevel: String = "Intermedio",
    val receiverName: String = "",
    val status: String = "PENDIENTE", // "PENDIENTE", "ACEPTADA", "RECHAZADA"
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerDetailedProfile(
    val name: String,
    val levelTier: String,
    val eloPoints: Int,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val preferredSide: String = "Revés", // "Revés", "Drive"
    val dominantHand: String = "Diestro",
    val favoriteRacket: String = "Babolat Technical Viper",
    val smashEffectiveness: Int = 85,
    val volleyEffectiveness: Int = 79,
    val bandejaEffectiveness: Int = 82,
    val wallExitEffectiveness: Int = 74,
    val avgPointsPerMatch: Float = 52.6f,
    val goldenPointsWon: Int = 14,
    val isFriend: Boolean = false
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (matchesWon.toFloat() / matchesPlayed.toFloat()) * 100f else 0f
}

data class MonthlyLevelProgressionPoint(
    val monthName: String,
    val levelValue: Float, // e.g. 2.6 to 4.3 (escala 1 a 7)
    val eloRating: Int, // e.g. 1250 to 1890
    val matchesPlayed: Int,
    val winRate: Float,
    val dominantStroke: String,
    val tacticalMilestone: String
)

@Entity(tableName = "social_players")
data class SocialPlayer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val level: String, // "Iniciación", "Intermedio", "Avanzado", "Pro"
    val points: Int,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val avatarColor: Int, // Int representation of color index
    val isCurrentUser: Boolean = false,
    val isFriend: Boolean = false
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (matchesWon.toFloat() / matchesPlayed.toFloat()) * 100 else 0f
}

@Entity(tableName = "friendly_challenges")
data class FriendlyChallenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val challengerName: String,
    val challengedName: String,
    val message: String,
    val status: String, // "Pendiente", "Aceptado", "Rechazado", "Completado"
    val dueDate: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SupportTicket(
    val id: String,
    val subject: String,
    val description: String,
    val category: String, // "Error de Puntuación", "Sincronización Reloj", "Ranking Social", "Otro"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Abierto"
)

data class PushNotificationLog(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "challenge", "ranking", "sync", "info"
)

@Entity(tableName = "video_analyses")
data class VideoAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val strokeType: String, // "Bandeja", "Víbora", "Remate x3", "Bajada de Pared", "Volea", "Saque", "Globo"
    val score: Int, // 0 to 100
    val levelTier: String, // e.g. "Intermedio Alto (Playtomic 4.2)"
    val positiveFeedback: String,
    val improvementFeedback: String,
    val technicalBreakdown: String,
    val recommendedDrill: String,
    val videoUriOrSample: String,
    val footworkScore: Int = 80,
    val impactScore: Int = 78,
    val followThroughScore: Int = 85,
    val tacticsScore: Int = 82,
    val timestamp: Long = System.currentTimeMillis()
)

data class PremierHighlight(
    val id: String,
    val tournament: String, // e.g., "Qatar Major Premier Padel 2026"
    val round: String, // e.g., "Gran Final Masculina"
    val pair1: String, // "Coello / Tapia"
    val pair2: String, // "Chingotto / Galán"
    val score: String, // "6-4, 5-7, 7-6"
    val duration: String, // "2h 45m"
    val highlightType: String, // "Puntazo del Torneo", "Remate x3 Imposible", "Salida de Pista", "Recuperación Milagrosa", "Tie-Break Infartante"
    val description: String,
    val keyPlaysCount: Int = 5,
    val videoDuration: String = "4:30 min",
    val tags: List<String> = listOf("Premier Padel", "Puntazos", "Final"),
    val likesCount: Int = 1420
)

data class WearableDevice(
    val id: String,
    val name: String,
    val address: String,
    val brand: String, // "Apple", "Garmin", "Wear OS / Samsung", "Polar", "Xiaomi", "Huawei"
    val deviceType: String, // "Smartwatch", "Banda Pectoral (HR)", "Pulsera Fitness"
    val batteryLevel: Int = 85,
    val rssi: Int = -62, // Signal dBm
    val isConnected: Boolean = false
)

data class BleSyncState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDevice: WearableDevice? = null,
    val liveHeartRateBpm: Int = 138,
    val heartRateZone: String = "Aeróbico (Cardio)", // "Calentamiento", "Quema Grasa", "Aeróbico", "Anaeróbico", "Máximo"
    val caloriesBurned: Int = 345,
    val packetsSynced: Int = 84,
    val isScoreSyncActive: Boolean = true,
    val lastSyncMessage: String = "Sincronizado vía BLE (GATT 0x180D/0x180F)"
)

data class HighlightReel(
    val id: String,
    val title: String,
    val matchTitle: String,
    val durationSec: Int = 45,
    val aspectRatio: String = "9:16 (Reels/TikTok)", // "9:16 (Reels/TikTok)" or "16:9 (Padel TV)"
    val selectedPointsCount: Int = 4,
    val musicTrack: String = "Premier Stadium Electro Beat",
    val clipsIncluded: List<String> = listOf(
        "Puntazo 38 golpes en la red",
        "Bajada de pared al cristal lateral",
        "Remate por 3 metros con efecto liftado",
        "Punto de partido decisivo y celebración"
    ),
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 342,
    val isAiGenerated: Boolean = true
)

data class FeedComment(
    val id: String,
    val authorName: String,
    val authorAvatarColor: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Hace un momento"
)

data class SharedFeedPost(
    val id: String,
    val analysisId: Int? = null,
    val authorName: String,
    val authorLevel: String,
    val authorAvatarColor: Int,
    val strokeType: String,
    val score: Int,
    val levelTier: String,
    val positiveFeedback: String,
    val improvementFeedback: String,
    val technicalBreakdown: String,
    val recommendedDrill: String,
    val caption: String,
    val videoUriOrSample: String = "bandeja",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 24,
    val isLikedByMe: Boolean = false,
    val comments: List<FeedComment> = emptyList(),
    val isSharedToProfile: Boolean = true,
    val playLink: String = "padelsync://analysis/$id"
)

/**
 * Report generated by FIP Padel Rule Validation engine
 */
data class MatchValidationReport(
    val isValid: Boolean,
    val ruleViolations: List<String> = emptyList(),
    val details: String = ""
)

/**
 * Entity for dedicated padel drill series and training sessions without playing a full match
 */
@Entity(tableName = "padel_drills")
data class PadelDrillSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val drillType: String, // "Voleas en Red", "Bandejas y Víboras", "Remates x3 / x4", "Bajadas de Pared", "Saques de Precisión", "Defensa y Globos"
    val targetReps: Int = 30,
    val successfulReps: Int = 24,
    val durationMinutes: Int = 15,
    val difficulty: String = "Intermedio", // "Iniciación", "Intermedio", "Competición"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val accuracyPercent: Float
        get() = if (targetReps > 0) (successfulReps.toFloat() / targetReps.toFloat()) * 100f else 0f
}


