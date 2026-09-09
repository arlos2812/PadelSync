package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors network availability in real-time and orchestrates
 * automatic synchronization between local Room database and Cloud Firestore
 * whenever internet connection is recovered.
 */
class PadelNetworkSyncManager(
    private val context: Context,
    private val onNetworkRestored: suspend () -> Unit
) {
    companion object {
        private const val TAG = "PadelNetworkSyncMgr"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isAutoSyncRunning = MutableStateFlow(false)
    val isAutoSyncRunning: StateFlow<Boolean> = _isAutoSyncRunning.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network connection acquired (ONLINE)")
            val wasOffline = !_isOnline.value
            _isOnline.value = true

            // When network is restored, automatically trigger synchronization
            if (wasOffline) {
                coroutineScope.launch {
                    try {
                        _isAutoSyncRunning.value = true
                        Log.i(TAG, "Triggering automatic Room -> Firestore sync upon connection restoration...")
                        onNetworkRestored()
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto sync on network recovery error: ${e.message}")
                    } finally {
                        _isAutoSyncRunning.value = false
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network connection lost (OFFLINE - Room local persistence active)")
            _isOnline.value = false
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            true // Fallback optimistic
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }
}
