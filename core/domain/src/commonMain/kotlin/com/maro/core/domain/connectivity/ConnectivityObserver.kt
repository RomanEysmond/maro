package com.maro.core.domain.connectivity

import kotlinx.coroutines.flow.Flow

/** Whether the device can reach the internet right now; used to resume syncing as soon as it can. */
interface ConnectivityObserver {
    /** Emits the current state at once, then every change. */
    val isConnected: Flow<Boolean>
}
