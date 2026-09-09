package com.example.util

import android.content.Context
import android.util.Log
import com.example.model.FriendRequest
import com.example.model.FriendlyChallenge
import com.example.model.PadelMatch
import com.example.model.SocialPlayer
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Service managing Firebase Auth and Cloud Firestore data persistence for user rankings,
 * profiles, and friends synchronization.
 */
class FirebaseRankingService(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseRankingService"
        private const val COLLECTION_RANKINGS = "rankings"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_CHALLENGES = "friendly_challenges"
        private const val COLLECTION_FRIEND_REQUESTS = "friend_requests"

        @Volatile
        private var instance: FirebaseRankingService? = null

        fun getInstance(context: Context): FirebaseRankingService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseRankingService(context.applicationContext).also { instance = it }
            }
        }
    }

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cloudRankings = MutableStateFlow<List<SocialPlayer>>(emptyList())
    val cloudRankings: StateFlow<List<SocialPlayer>> = _cloudRankings.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Iniciando conexión con Firebase...")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            _isFirebaseConnected.value = true
            _syncStatusMessage.value = "Conectado a Firebase Cloud Firestore"
            _currentUser.value = auth?.currentUser

            // Listen for auth state changes
            auth?.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }

            listenToCloudRankings()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not yet initialized or credentials missing; using offline-first resilient mode: ${e.message}")
            _isFirebaseConnected.value = false
            _syncStatusMessage.value = "Modo local / Caché lista (Firebase inicializándose)"
        }
    }

    /**
     * Listen in real-time to Firestore rankings collection.
     */
    fun listenToCloudRankings() {
        val db = firestore ?: return
        try {
            snapshotListener?.remove()
            snapshotListener = db.collection(COLLECTION_RANKINGS)
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to Firestore rankings", error)
                        _syncStatusMessage.value = "Sincronizado desde caché local"
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val playerList = snapshots.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.getLong("id") ?: 0L).toInt()
                                val name = doc.getString("name") ?: "Jugador"
                                val level = doc.getString("level") ?: "Intermedio"
                                val points = (doc.getLong("points") ?: 1000L).toInt()
                                val matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt()
                                val matchesWon = (doc.getLong("matchesWon") ?: 0L).toInt()
                                val avatarColor = (doc.getLong("avatarColor") ?: 0L).toInt()
                                val isCurrentUser = doc.getBoolean("isCurrentUser") ?: false
                                val isFriend = doc.getBoolean("isFriend") ?: false

                                SocialPlayer(
                                    id = if (id > 0) id else doc.id.hashCode(),
                                    name = name,
                                    level = level,
                                    points = points,
                                    matchesPlayed = matchesPlayed,
                                    matchesWon = matchesWon,
                                    avatarColor = avatarColor,
                                    isCurrentUser = isCurrentUser,
                                    isFriend = isFriend
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _cloudRankings.value = playerList
                        _isFirebaseConnected.value = true
                        _syncStatusMessage.value = "Sincronizado con Firebase Firestore (${playerList.size} jugadores)"
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach snapshot listener to Firestore: ${e.message}")
        }
    }

    /**
     * Manually sync local players up to Firestore and pull down remote rankings.
     */
    suspend fun syncRankingsWithCloud(localPlayers: List<SocialPlayer>): List<SocialPlayer> {
        val db = firestore ?: return localPlayers
        _isSyncing.value = true
        _syncStatusMessage.value = "Sincronizando con Firebase Firestore..."

        return try {
            val batch = db.batch()
            // If cloud is empty, seed with local players
            val snapshot = db.collection(COLLECTION_RANKINGS).limit(1).get().await()
            if (snapshot.isEmpty && localPlayers.isNotEmpty()) {
                localPlayers.forEach { player ->
                    val docRef = db.collection(COLLECTION_RANKINGS).document("player_${player.id}")
                    val data = mapOf(
                        "id" to player.id,
                        "name" to player.name,
                        "level" to player.level,
                        "points" to player.points,
                        "matchesPlayed" to player.matchesPlayed,
                        "matchesWon" to player.matchesWon,
                        "avatarColor" to player.avatarColor,
                        "isCurrentUser" to player.isCurrentUser,
                        "isFriend" to player.isFriend,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }

            // Fetch current rankings from Firestore
            val cloudSnapshot = db.collection(COLLECTION_RANKINGS)
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val remoteList = cloudSnapshot.documents.mapNotNull { doc ->
                try {
                    val id = (doc.getLong("id") ?: 0L).toInt()
                    val name = doc.getString("name") ?: "Jugador"
                    val level = doc.getString("level") ?: "Intermedio"
                    val points = (doc.getLong("points") ?: 1000L).toInt()
                    val matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt()
                    val matchesWon = (doc.getLong("matchesWon") ?: 0L).toInt()
                    val avatarColor = (doc.getLong("avatarColor") ?: 0L).toInt()
                    val isCurrentUser = doc.getBoolean("isCurrentUser") ?: false
                    val isFriend = doc.getBoolean("isFriend") ?: false

                    SocialPlayer(
                        id = if (id > 0) id else doc.id.hashCode(),
                        name = name,
                        level = level,
                        points = points,
                        matchesPlayed = matchesPlayed,
                        matchesWon = matchesWon,
                        avatarColor = avatarColor,
                        isCurrentUser = isCurrentUser,
                        isFriend = isFriend
                    )
                } catch (e: Exception) {
                    null
                }
            }

            _isFirebaseConnected.value = true
            _syncStatusMessage.value = "Ranking actualizado desde Firebase (${remoteList.size} usuarios)"
            _cloudRankings.value = remoteList
            _isSyncing.value = false
            if (remoteList.isNotEmpty()) remoteList else localPlayers
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync completed with fallback to local: ${e.message}")
            _syncStatusMessage.value = "Modo local activo (Caché sincronizada)"
            _isSyncing.value = false
            localPlayers
        }
    }

    /**
     * Updates friend status in Firestore.
     */
    suspend fun setPlayerFriendStatusInFirestore(player: SocialPlayer, isFriend: Boolean) {
        val db = firestore ?: return
        try {
            val docRef = db.collection(COLLECTION_RANKINGS).document("player_${player.id}")
            docRef.update("isFriend", isFriend).await()
            Log.d(TAG, "Updated friend status in Firestore for player ${player.name} to $isFriend")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update friend status in Firestore: ${e.message}")
        }
    }

    /**
     * Saves or updates the current user's profile and stats in Firestore.
     */
    suspend fun saveUserProfileToFirestore(player: SocialPlayer, email: String) {
        val db = firestore ?: return
        try {
            val docRef = db.collection(COLLECTION_RANKINGS).document("player_${player.id}")
            val data = mapOf(
                "id" to player.id,
                "name" to player.name,
                "level" to player.level,
                "points" to player.points,
                "matchesPlayed" to player.matchesPlayed,
                "matchesWon" to player.matchesWon,
                "avatarColor" to player.avatarColor,
                "isCurrentUser" to true,
                "isFriend" to false,
                "email" to email,
                "lastActive" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            _syncStatusMessage.value = "Perfil sincronizado en Firebase"
        } catch (e: Exception) {
            Log.w(TAG, "Could not save user profile to Firestore: ${e.message}")
        }
    }

    /**
     * Firebase Auth sign in with Google ID Token credential
     */
    suspend fun signInWithGoogleCredential(idToken: String, email: String, name: String): Boolean {
        val firebaseAuth = auth ?: return false
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            _currentUser.value = authResult.user
            _isFirebaseConnected.value = true
            _syncStatusMessage.value = "Sesión iniciada con Google en Firebase"
            Log.d(TAG, "Successfully authenticated with Google in Firebase: ${authResult.user?.uid}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Google Auth fallback: ${e.message}")
            signInWithFirebaseAuth(email, name)
            true
        }
    }

    /**
     * Firebase Auth sign in anonymously or with email/Google token.
     */
    suspend fun signInWithFirebaseAuth(email: String, name: String) {
        val firebaseAuth = auth ?: return
        try {
            if (firebaseAuth.currentUser == null) {
                firebaseAuth.signInAnonymously().await()
                _currentUser.value = firebaseAuth.currentUser
                _isFirebaseConnected.value = true
                Log.d(TAG, "Signed in to Firebase Auth as ${firebaseAuth.currentUser?.uid}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth sign in notice: ${e.message}")
        }
    }

    /**
     * Save user padel match history and aggregated statistics to Cloud Firestore
     */
    suspend fun syncUserMatchesAndStats(
        userId: String,
        email: String,
        name: String,
        matches: List<PadelMatch>
    ): Boolean {
        val db = firestore ?: return false
        _isSyncing.value = true
        return try {
            val docId = if (userId.isNotBlank()) userId else "user_${email.hashCode()}"
            val userDoc = db.collection(COLLECTION_USERS).document(docId)

            val userMatches = matches.filter {
                it.player1A.equals(name, ignoreCase = true) || it.player1B.equals(name, ignoreCase = true) ||
                it.player2A.equals(name, ignoreCase = true) || it.player2B.equals(name, ignoreCase = true) ||
                it.player1A == "Yo (Tú)" || it.player1B == "Yo (Tú)" ||
                it.player2A == "Yo (Tú)" || it.player2B == "Yo (Tú)"
            }

            val won = userMatches.count {
                val isP1 = it.player1A.equals(name, ignoreCase = true) || it.player1B.equals(name, ignoreCase = true) ||
                           it.player1A == "Yo (Tú)" || it.player1B == "Yo (Tú)"
                if (isP1) it.winnerTeam == 1 else it.winnerTeam == 2
            }
            val lost = userMatches.size - won
            val winRate = if (userMatches.isNotEmpty()) (won.toFloat() / userMatches.size.toFloat()) * 100f else 0f

            val statsData = mapOf(
                "email" to email,
                "name" to name,
                "matchesPlayed" to userMatches.size,
                "matchesWon" to won,
                "matchesLost" to lost,
                "winRate" to winRate,
                "lastSyncTimestamp" to System.currentTimeMillis()
            )
            userDoc.set(statsData, SetOptions.merge()).await()

            for (match in userMatches.take(50)) {
                val matchDoc = userDoc.collection("matches").document("match_${match.id}")
                val matchData = mapOf(
                    "id" to match.id,
                    "player1A" to match.player1A,
                    "player1B" to match.player1B,
                    "player2A" to match.player2A,
                    "player2B" to match.player2B,
                    "setsTeam1" to match.setsTeam1,
                    "setsTeam2" to match.setsTeam2,
                    "winnerTeam" to match.winnerTeam,
                    "tournamentName" to match.tournamentName,
                    "matchFormat" to match.matchFormat,
                    "timestamp" to match.timestamp
                )
                matchDoc.set(matchData, SetOptions.merge()).await()
            }

            _syncStatusMessage.value = "Estadísticas y ${userMatches.size} partidos sincronizados en Firebase Cloud"
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing matches to Firestore: ${e.message}")
            _syncStatusMessage.value = "Guardado localmente en Room (modo sin conexión)"
            false
        } finally {
            _isSyncing.value = false
        }
    }

    private var challengesListener: ListenerRegistration? = null
    private val _cloudChallenges = MutableStateFlow<List<FriendlyChallenge>>(emptyList())
    val cloudChallenges: StateFlow<List<FriendlyChallenge>> = _cloudChallenges.asStateFlow()

    /**
     * Send a match challenge to a contact and persist it in Firebase Firestore.
     */
    suspend fun sendMatchChallenge(challenge: FriendlyChallenge): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (challenge.id > 0) "challenge_${challenge.id}" else "challenge_${challenge.timestamp}"
            val docRef = db.collection(COLLECTION_CHALLENGES).document(docId)
            val data = mapOf(
                "id" to challenge.id,
                "challengerName" to challenge.challengerName,
                "challengedName" to challenge.challengedName,
                "message" to challenge.message,
                "status" to challenge.status, // "Pendiente", "Aceptado", "Rechazado", "Completado"
                "dueDate" to challenge.dueDate,
                "timestamp" to challenge.timestamp
            )
            docRef.set(data, SetOptions.merge()).await()
            _syncStatusMessage.value = "Reto enviado a Firestore (${challenge.challengedName})"
            Log.d(TAG, "Challenge sent to Firestore: $docId")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error sending challenge to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Update challenge status (e.g. Aceptado, Rechazado) in Firebase Firestore.
     */
    suspend fun updateChallengeStatus(challengeId: String, newStatus: String): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection(COLLECTION_CHALLENGES).document(challengeId)
            docRef.update("status", newStatus).await()
            _syncStatusMessage.value = "Reto actualizado: $newStatus"
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error updating challenge in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Listen in real-time to match challenges in Firestore.
     */
    fun listenToCloudChallenges(userName: String, onUpdate: (List<FriendlyChallenge>) -> Unit) {
        val db = firestore ?: return
        try {
            challengesListener?.remove()
            challengesListener = db.collection(COLLECTION_CHALLENGES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Challenges listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        val list = snapshots.documents.mapNotNull { doc ->
                            try {
                                FriendlyChallenge(
                                    id = (doc.getLong("id") ?: 0L).toInt(),
                                    challengerName = doc.getString("challengerName") ?: "",
                                    challengedName = doc.getString("challengedName") ?: "",
                                    message = doc.getString("message") ?: "",
                                    status = doc.getString("status") ?: "Pendiente",
                                    dueDate = doc.getString("dueDate") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _cloudChallenges.value = list
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching challenges listener: ${e.message}")
        }
    }

    private var friendRequestsListener: ListenerRegistration? = null
    private val _cloudFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val cloudFriendRequests: StateFlow<List<FriendRequest>> = _cloudFriendRequests.asStateFlow()

    /**
     * Send a friend request to a padel player via Firebase Firestore.
     */
    suspend fun sendFriendRequestToFirestore(senderName: String, senderLevel: String, receiverName: String): Boolean {
        val db = firestore ?: return false
        return try {
            val reqId = "req_${System.currentTimeMillis()}"
            val docRef = db.collection(COLLECTION_FRIEND_REQUESTS).document(reqId)
            val data = mapOf(
                "id" to reqId,
                "senderName" to senderName,
                "senderLevel" to senderLevel,
                "receiverName" to receiverName,
                "status" to "PENDIENTE",
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            _syncStatusMessage.value = "Solicitud de amistad enviada a $receiverName"
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error sending friend request: ${e.message}")
            false
        }
    }

    /**
     * Respond to a friend request (Aceptar / Rechazar) in Firestore.
     */
    suspend fun respondToFriendRequestInFirestore(requestId: String, accept: Boolean): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection(COLLECTION_FRIEND_REQUESTS).document(requestId)
            val newStatus = if (accept) "ACEPTADA" else "RECHAZADA"
            docRef.update("status", newStatus).await()
            _syncStatusMessage.value = if (accept) "¡Solicitud aceptada! Ahora son amigos" else "Solicitud rechazada"
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error updating friend request: ${e.message}")
            false
        }
    }

    /**
     * Listen to friend requests in real-time.
     */
    fun listenToCloudFriendRequests(userName: String, onUpdate: (List<FriendRequest>) -> Unit) {
        val db = firestore ?: return
        try {
            friendRequestsListener?.remove()
            friendRequestsListener = db.collection(COLLECTION_FRIEND_REQUESTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Friend requests listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        val list = snapshots.documents.mapNotNull { doc ->
                            try {
                                FriendRequest(
                                    id = doc.getString("id") ?: doc.id,
                                    senderName = doc.getString("senderName") ?: "",
                                    senderLevel = doc.getString("senderLevel") ?: "Intermedio",
                                    receiverName = doc.getString("receiverName") ?: "",
                                    status = doc.getString("status") ?: "PENDIENTE",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _cloudFriendRequests.value = list
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error listening to friend requests: ${e.message}")
        }
    }

    /**
     * Confirm match score in Firestore by a player (Yo, compañero, o rivales).
     */
    suspend fun confirmMatchScoreInFirestore(matchId: String, playerName: String, allConfirmed: Boolean): Boolean {
        val db = firestore ?: return false
        return try {
            val docRef = db.collection("matches").document(matchId)
            val updates = mutableMapOf<String, Any>(
                "confirmedBy_${playerName.replace(" ", "_")}" to true,
                "lastConfirmedBy" to playerName,
                "isScoreConfirmed" to allConfirmed
            )
            docRef.set(updates, SetOptions.merge()).await()
            _syncStatusMessage.value = "Marcador ratificado por $playerName en Firestore"
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error confirming score in Firestore: ${e.message}")
            false
        }
    }

    fun cleanup() {
        snapshotListener?.remove()
        challengesListener?.remove()
        friendRequestsListener?.remove()
    }
}
