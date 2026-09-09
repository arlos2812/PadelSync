package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Manager for posting local Android system notifications:
 * - Match reminders for scheduled padel matches
 * - Instant alerts when a friend accepts a challenge
 */
object PadelNotificationManager {

    private const val TAG = "PadelNotificationMgr"

    const val CHANNEL_MATCH_REMINDERS = "padel_match_reminders"
    const val CHANNEL_CHALLENGE_ALERTS = "padel_challenge_alerts"
    const val CHANNEL_MATCH_INVITATIONS = "padel_match_invitations"
    const val CHANNEL_SCORE_CONFIRMATION = "padel_score_confirmations"

    private const val NOTIF_ID_MATCH_REMINDER = 1001
    private const val NOTIF_ID_CHALLENGE_ALERT = 1002
    private const val NOTIF_ID_MATCH_INVITATION = 1003
    private const val NOTIF_ID_SCORE_CONFIRMED = 1004

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Scheduled match reminders
            val matchChannel = NotificationChannel(
                CHANNEL_MATCH_REMINDERS,
                "Recordatorios de Partidos Programados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de partidos programados de pádel y torneos próximos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }

            // Channel 2: Challenge alerts
            val challengeChannel = NotificationChannel(
                CHANNEL_CHALLENGE_ALERTS,
                "Alertas de Desafíos y Retos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un amigo acepta tu reto de pádel"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }

            // Channel 3: Match invitations
            val invitationChannel = NotificationChannel(
                CHANNEL_MATCH_INVITATIONS,
                "Invitaciones a Partidos de Pádel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos instantáneos cuando un jugador o pareja te invita a un nuevo partido"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 300)
                setShowBadge(true)
            }

            // Channel 4: Score confirmations
            val scoreChannel = NotificationChannel(
                CHANNEL_SCORE_CONFIRMATION,
                "Confirmaciones de Marcador y Actas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos cuando un rival ratifica y confirma el resultado oficial del partido"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 100, 350)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(matchChannel)
            notificationManager.createNotificationChannel(challengeChannel)
            notificationManager.createNotificationChannel(invitationChannel)
            notificationManager.createNotificationChannel(scoreChannel)
            Log.d(TAG, "Notification channels initialized successfully")
        }
    }

    /**
     * Checks if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Sends a local notification reminding the user of a scheduled padel match
     */
    fun sendScheduledMatchReminder(
        context: Context,
        matchTitle: String,
        courtName: String,
        timeText: String,
        matchId: Int = 1
    ) {
        initNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("nav_destination", "matches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                matchId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_MATCH_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🎾 Partido Programado: $matchTitle")
                .setContentText("Hoy a las $timeText en $courtName. ¡Prepara la pala y a calentar!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Tienes un partido programado en $courtName para hoy a las $timeText.\n" +
                        "Formato oficial, calentamiento previo recomendado de 10 min. ¡Suerte en la pista!"
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIF_ID_MATCH_REMINDER + matchId, builder.build())
            Log.d(TAG, "Scheduled match notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending match notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending match notification: ${e.message}")
        }
    }

    /**
     * Sends a local notification alert when a friend accepts a padel challenge
     */
    fun sendChallengeAcceptedAlert(
        context: Context,
        friendName: String,
        challengeMessage: String,
        challengeId: Int = 1
    ) {
        initNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("nav_destination", "social")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                2000 + challengeId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_CHALLENGE_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🏆 ¡Desafío Aceptado por $friendName!")
                .setContentText("Ha aceptado tu reto: \"$challengeMessage\". ¡Hora de demostrar tu nivel!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "¡Buenas noticias! $friendName ha aceptado tu desafío de pádel.\n" +
                        "Mensaje: \"$challengeMessage\"\n" +
                        "Abre PadelSync para coordinar fecha, pista y comenzar el partido."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIF_ID_CHALLENGE_ALERT + challengeId, builder.build())
            Log.d(TAG, "Challenge accepted notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending challenge notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending challenge notification: ${e.message}")
        }
    }

    /**
     * Sends a local notification alert when the user has been invited to a new padel match
     */
    fun sendMatchInvitationAlert(
        context: Context,
        hostPlayerName: String,
        courtName: String,
        dateTimeText: String,
        matchId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        initNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("nav_destination", "matches")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                3000 + matchId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_MATCH_INVITATIONS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🎾 ¡Convocatoria: Invitación a Partido!")
                .setContentText("$hostPlayerName te ha invitado a jugar en $courtName ($dateTimeText).")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "¡Tienes una nueva invitación a pista!\n" +
                        "Organizador: $hostPlayerName\n" +
                        "Pista: $courtName\n" +
                        "Horario: $dateTimeText\n" +
                        "Toca para abrir la convocatoria y confirmar tu plaza en la alineación."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIF_ID_MATCH_INVITATION + matchId, builder.build())
            Log.d(TAG, "Match invitation notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending invitation notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending invitation notification: ${e.message}")
        }
    }

    /**
     * Sends a local notification alert when an opponent or rival player has confirmed and ratified the match score
     */
    fun sendScoreConfirmedAlert(
        context: Context,
        opponentName: String,
        finalScore: String,
        courtName: String = "Pista Oficial",
        matchId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        initNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("nav_destination", "history")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                4000 + matchId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_SCORE_CONFIRMATION)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("✅ ¡Marcador Ratificado por Rival!")
                .setContentText("$opponentName ha confirmado el resultado final ($finalScore).")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "¡Acta oficial del partido cerrada!\n" +
                        "Rival: $opponentName ha firmado el acta.\n" +
                        "Resultado final: $finalScore ($courtName).\n" +
                        "Tus puntos ELO, historial y estadísticas han sido homologados."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIF_ID_SCORE_CONFIRMED + matchId, builder.build())
            Log.d(TAG, "Score confirmed notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending score confirmation notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending score confirmation notification: ${e.message}")
        }
    }
}
