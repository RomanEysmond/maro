package com.maro.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.maro.core.domain.connectivity.ConnectivityObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Follows the default network; "connected" means Android has validated that it actually reaches the internet. */
internal class AndroidConnectivityObserver(
    context: Context,
) : ConnectivityObserver {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.isOnline())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }
        trySend(connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)?.isOnline() == true)
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()

    private fun NetworkCapabilities.isOnline(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
