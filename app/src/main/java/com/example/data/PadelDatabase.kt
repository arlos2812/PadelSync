package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.model.FriendlyChallenge
import com.example.model.PadelDrillSession
import com.example.model.PadelMatch
import com.example.model.SocialPlayer
import com.example.model.VideoAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface PadelMatchDao {
    @Query("SELECT * FROM padel_matches ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<PadelMatch>>

    @Query("SELECT * FROM padel_matches WHERE isSynced = 0")
    fun getUnsyncedMatchesFlow(): Flow<List<PadelMatch>>

    @Query("SELECT * FROM padel_matches WHERE isSynced = 0")
    suspend fun getUnsyncedMatchesList(): List<PadelMatch>

    @Query("UPDATE padel_matches SET isSynced = 1 WHERE id IN (:matchIds)")
    suspend fun markMatchesAsSynced(matchIds: List<Int>)

    @Query("UPDATE padel_matches SET isSynced = 1 WHERE id = :matchId")
    suspend fun markMatchAsSynced(matchId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: PadelMatch): Long

    @Update
    suspend fun updateMatch(match: PadelMatch)

    @Query("DELETE FROM padel_matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Int)

    @Query("SELECT * FROM padel_matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Int): PadelMatch?
}

@Dao
interface SocialPlayerDao {
    @Query("SELECT * FROM social_players ORDER BY points DESC")
    fun getAllPlayers(): Flow<List<SocialPlayer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: SocialPlayer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<SocialPlayer>)

    @Update
    suspend fun updatePlayer(player: SocialPlayer)

    @Query("SELECT * FROM social_players WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserPlayer(): Flow<SocialPlayer?>

    @Query("UPDATE social_players SET points = points + :pointsChange, matchesPlayed = matchesPlayed + 1, matchesWon = matchesWon + :wonChange WHERE name = :playerName")
    suspend fun updatePlayerStats(playerName: String, pointsChange: Int, wonChange: Int)

    @Query("UPDATE social_players SET isFriend = :isFriend WHERE id = :playerId")
    suspend fun updatePlayerFriendStatus(playerId: Int, isFriend: Boolean)
}

@Dao
interface FriendlyChallengeDao {
    @Query("SELECT * FROM friendly_challenges ORDER BY timestamp DESC")
    fun getAllChallenges(): Flow<List<FriendlyChallenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: FriendlyChallenge): Long

    @Update
    suspend fun updateChallenge(challenge: FriendlyChallenge)

    @Query("UPDATE friendly_challenges SET status = :status WHERE id = :challengeId")
    suspend fun updateChallengeStatus(challengeId: Int, status: String)
}

@Dao
interface VideoAnalysisDao {
    @Query("SELECT * FROM video_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<VideoAnalysis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: VideoAnalysis): Long

    @Query("DELETE FROM video_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysisById(analysisId: Int)

    @Query("SELECT * FROM video_analyses WHERE id = :analysisId LIMIT 1")
    suspend fun getAnalysisById(analysisId: Int): VideoAnalysis?
}

@Dao
interface PadelDrillDao {
    @Query("SELECT * FROM padel_drills ORDER BY timestamp DESC")
    fun getAllDrills(): Flow<List<PadelDrillSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrill(drill: PadelDrillSession): Long

    @Update
    suspend fun updateDrill(drill: PadelDrillSession)

    @Query("DELETE FROM padel_drills WHERE id = :drillId")
    suspend fun deleteDrillById(drillId: Int)

    @Query("SELECT * FROM padel_drills WHERE id = :drillId LIMIT 1")
    suspend fun getDrillById(drillId: Int): PadelDrillSession?
}

@Database(
    entities = [PadelMatch::class, SocialPlayer::class, FriendlyChallenge::class, VideoAnalysis::class, PadelDrillSession::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun padelMatchDao(): PadelMatchDao
    abstract fun socialPlayerDao(): SocialPlayerDao
    abstract fun friendlyChallengeDao(): FriendlyChallengeDao
    abstract fun videoAnalysisDao(): VideoAnalysisDao
    abstract fun padelDrillDao(): PadelDrillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "padel_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PadelRepository(
    private val matchDao: PadelMatchDao,
    private val playerDao: SocialPlayerDao,
    private val challengeDao: FriendlyChallengeDao,
    private val videoDao: VideoAnalysisDao,
    private val drillDao: PadelDrillDao
) {
    val allMatches: Flow<List<PadelMatch>> = matchDao.getAllMatches()
    val allPlayers: Flow<List<SocialPlayer>> = playerDao.getAllPlayers()
    val allChallenges: Flow<List<FriendlyChallenge>> = challengeDao.getAllChallenges()
    val allAnalyses: Flow<List<VideoAnalysis>> = videoDao.getAllAnalyses()
    val allDrills: Flow<List<PadelDrillSession>> = drillDao.getAllDrills()
    val currentUserPlayer: Flow<SocialPlayer?> = playerDao.getCurrentUserPlayer()
    val unsyncedMatchesFlow: Flow<List<PadelMatch>> = matchDao.getUnsyncedMatchesFlow()

    suspend fun getMatchById(matchId: Int): PadelMatch? = matchDao.getMatchById(matchId)

    suspend fun insertMatch(match: PadelMatch): Long = matchDao.insertMatch(match)

    suspend fun updateMatch(match: PadelMatch) = matchDao.updateMatch(match)

    suspend fun deleteMatchById(matchId: Int) = matchDao.deleteMatchById(matchId)

    suspend fun getUnsyncedMatches(): List<PadelMatch> = matchDao.getUnsyncedMatchesList()

    suspend fun markMatchesAsSynced(matchIds: List<Int>) = matchDao.markMatchesAsSynced(matchIds)

    suspend fun markMatchAsSynced(matchId: Int) = matchDao.markMatchAsSynced(matchId)

    suspend fun insertPlayer(player: SocialPlayer) = playerDao.insertPlayer(player)

    suspend fun populateInitialPlayers(players: List<SocialPlayer>) = playerDao.insertPlayers(players)

    suspend fun updatePlayer(player: SocialPlayer) = playerDao.updatePlayer(player)

    suspend fun updatePlayerStats(playerName: String, pointsChange: Int, won: Boolean) {
        playerDao.updatePlayerStats(playerName, pointsChange, if (won) 1 else 0)
    }

    suspend fun updatePlayerFriendStatus(playerId: Int, isFriend: Boolean) {
        playerDao.updatePlayerFriendStatus(playerId, isFriend)
    }

    suspend fun insertChallenge(challenge: FriendlyChallenge): Long = challengeDao.insertChallenge(challenge)

    suspend fun updateChallengeStatus(challengeId: Int, status: String) = challengeDao.updateChallengeStatus(challengeId, status)

    suspend fun insertAnalysis(analysis: VideoAnalysis): Long = videoDao.insertAnalysis(analysis)

    suspend fun deleteAnalysisById(analysisId: Int) = videoDao.deleteAnalysisById(analysisId)

    suspend fun getAnalysisById(analysisId: Int): VideoAnalysis? = videoDao.getAnalysisById(analysisId)

    suspend fun insertDrill(drill: PadelDrillSession): Long = drillDao.insertDrill(drill)

    suspend fun deleteDrillById(drillId: Int) = drillDao.deleteDrillById(drillId)

    suspend fun getDrillById(drillId: Int): PadelDrillSession? = drillDao.getDrillById(drillId)
}
