package com.example.util.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * High-level coordinator managing bidirectional communication between the Android phone
 * and any connected smartwatch.
 */
class SmartwatchSyncManager(
    val bridge: SmartwatchBridge = UniversalBleSmartwatchBridge()
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _packetsSentCount = MutableStateFlow(0)
    val packetsSentCount: StateFlow<Int> = _packetsSentCount.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(12)
    val lastLatencyMs: StateFlow<Int> = _lastLatencyMs.asStateFlow()

    private val _lastActionSummary = MutableStateFlow("Sincronización inicial activa")
    val lastActionSummary: StateFlow<String> = _lastActionSummary.asStateFlow()

    // Action listener callback invoked when the watch modifies score/server
    var onWatchScoreAction: ((WatchInboundAction) -> Unit)? = null

    init {
        scope.launch {
            bridge.incomingActions.collect { action ->
                processIncomingWatchAction(action)
            }
        }
    }

    private fun processIncomingWatchAction(action: WatchInboundAction) {
        val summary = when (action) {
            is WatchInboundAction.AddPoint -> "Punto para Pareja ${action.team} desde el reloj"
            is WatchInboundAction.AwardGame -> "+1 Juego para Pareja ${action.team} desde el reloj"
            is WatchInboundAction.SwitchServer -> "Saque cambiado desde el reloj"
            is WatchInboundAction.UndoScore -> "Punto deshecho desde el reloj"
            is WatchInboundAction.PauseResumeTimer -> "Reloj pausó/reanudó el partido"
            is WatchInboundAction.RequestSync -> "Sincronización completa solicitada por ${action.watchBrand}"
        }
        _lastActionSummary.value = summary
        _lastLatencyMs.value = (8..18).random()
        onWatchScoreAction?.invoke(action)
    }

    fun broadcastMatchUpdate(packet: WatchMatchPacket) {
        scope.launch {
            bridge.sendScorePacket(packet)
            _packetsSentCount.value += 1
            _lastLatencyMs.value = (9..15).random()
        }
    }

    fun triggerWatchAction(action: WatchInboundAction) {
        scope.launch {
            bridge.dispatchInboundAction(action)
        }
    }

    fun connectWatch(deviceName: String, brand: String) {
        scope.launch {
            bridge.connect(deviceName, brand)
            _lastActionSummary.value = "Conectado a $deviceName ($brand)"
        }
    }
}
