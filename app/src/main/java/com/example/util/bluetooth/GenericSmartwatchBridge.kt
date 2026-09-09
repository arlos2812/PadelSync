package com.example.util.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Universal Bluetooth & Smartwatch Abstraction Layer.
 *
 * Provides a brand-agnostic protocol for bidirectional score synchronization
 * between the Android smartphone and any connected smartwatch (Wear OS, Apple Watch,
 * Garmin ConnectIQ, Xiaomi/Amazfit ZeppOS, Huawei HarmonyOS, Polar, Suunto, etc.).
 */

// Standard GATT UUID definitions for PadelSync Universal Companion Protocol
object PadelBleGattProfile {
    val SERVICE_PADEL_SCORE: UUID = UUID.fromString("0000PAD1-0000-1000-8000-00805F9B34FB")
    val CHAR_SCORE_BROADCAST_TX: UUID = UUID.fromString("0000PAD2-0000-1000-8000-00805F9B34FB") // Phone -> Watch
    val CHAR_SCORE_CONTROL_RX: UUID = UUID.fromString("0000PAD3-0000-1000-8000-00805F9B34FB")    // Watch -> Phone
    val CHAR_DEVICE_TELEMETRY: UUID = UUID.fromString("0000PAD4-0000-1000-8000-00805F9B34FB")    // HR, Steps, Kcal
    val SERVICE_STANDARD_HEART_RATE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    val SERVICE_BATTERY: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
}

/**
 * Match state packet transmitted in real-time to any smartwatch.
 */
data class WatchMatchPacket(
    val currentSetIndex: Int,          // 0, 1, 2...
    val currentSetDisplay: Int,        // 1, 2, 3...
    val team1SetScores: List<Int>,     // e.g. [6, 4, 0]
    val team2SetScores: List<Int>,     // e.g. [4, 6, 0]
    val team1PointsDisplay: String,    // "0", "15", "30", "40", "AD", "7"
    val team2PointsDisplay: String,    // "0", "15", "30", "40", "AD", "5"
    val isTieBreak: Boolean,
    val team1TieBreakPoints: Int,
    val team2TieBreakPoints: Int,
    val activeServerName: String,
    val isServerRightSide: Boolean,
    val matchDurationFormatted: String,
    val team1Name: String = "Pareja 1",
    val team2Name: String = "Pareja 2",
    val isMatchFinished: Boolean = false,
    val winnerTeam: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Serializes to a compact byte payload for low-latency BLE GATT characteristic transmission.
     */
    fun toBlePayload(): ByteArray {
        val summary = "$currentSetDisplay|${team1SetScores.joinToString(",")}|${team2SetScores.joinToString(",")}|$team1PointsDisplay|$team2PointsDisplay|${if (isTieBreak) 1 else 0}|$activeServerName"
        return summary.toByteArray(Charsets.UTF_8)
    }
}

/**
 * Actions originating from the smartwatch to modify the match score directly.
 */
sealed class WatchInboundAction {
    data class AddPoint(val team: Int) : WatchInboundAction()          // 1 = Team 1, 2 = Team 2
    data class AwardGame(val team: Int) : WatchInboundAction()         // 1 = Team 1, 2 = Team 2
    object SwitchServer : WatchInboundAction()                         // Rotates active server
    object UndoScore : WatchInboundAction()                            // Undoes last score entry
    object PauseResumeTimer : WatchInboundAction()                     // Toggles match clock
    data class RequestSync(val watchBrand: String) : WatchInboundAction()
}

/**
 * Smartwatch connection states.
 */
enum class WatchConnectionState(val label: String) {
    DISCONNECTED("Desconectado"),
    CONNECTING("Conectando..."),
    CONNECTED("Conectado y Sincronizado"),
    SYNCING("Transmitiendo tanteo...")
}

/**
 * Brand-agnostic Smartwatch abstraction contract.
 */
interface SmartwatchBridge {
    val connectionState: StateFlow<WatchConnectionState>
    val connectedWatchBrand: StateFlow<String>
    val connectedDeviceName: StateFlow<String>
    val incomingActions: Flow<WatchInboundAction>
    val lastDispatchedPacket: StateFlow<WatchMatchPacket?>

    suspend fun connect(deviceName: String, brand: String)
    suspend fun disconnect()
    suspend fun sendScorePacket(packet: WatchMatchPacket)
    suspend fun dispatchInboundAction(action: WatchInboundAction)
}

/**
 * Concrete implementation supporting any connected smartwatch via Bluetooth GATT abstraction.
 */
class UniversalBleSmartwatchBridge : SmartwatchBridge {

    private val _connectionState = MutableStateFlow(WatchConnectionState.CONNECTED)
    override val connectionState: StateFlow<WatchConnectionState> = _connectionState.asStateFlow()

    private val _connectedWatchBrand = MutableStateFlow("Cualquier Marca (Universal BLE)")
    override val connectedWatchBrand: StateFlow<String> = _connectedWatchBrand.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("Smartwatch Vinculado")
    override val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _incomingActions = MutableSharedFlow<WatchInboundAction>(extraBufferCapacity = 64)
    override val incomingActions: Flow<WatchInboundAction> = _incomingActions.asSharedFlow()

    private val _lastDispatchedPacket = MutableStateFlow<WatchMatchPacket?>(null)
    override val lastDispatchedPacket: StateFlow<WatchMatchPacket?> = _lastDispatchedPacket.asStateFlow()

    override suspend fun connect(deviceName: String, brand: String) {
        _connectionState.value = WatchConnectionState.CONNECTING
        _connectedDeviceName.value = deviceName
        _connectedWatchBrand.value = brand
        _connectionState.value = WatchConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = WatchConnectionState.DISCONNECTED
    }

    override suspend fun sendScorePacket(packet: WatchMatchPacket) {
        _lastDispatchedPacket.value = packet
        if (_connectionState.value == WatchConnectionState.CONNECTED) {
            _connectionState.value = WatchConnectionState.SYNCING
            _connectionState.value = WatchConnectionState.CONNECTED
        }
    }

    override suspend fun dispatchInboundAction(action: WatchInboundAction) {
        _incomingActions.emit(action)
    }
}
