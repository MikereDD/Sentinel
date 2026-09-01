package com.typezero.sentinel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.typezero.sentinel.data.db.KnownDevice
import com.typezero.sentinel.data.db.NetworkEvent
import com.typezero.sentinel.data.db.WatchedTarget
import com.typezero.sentinel.data.model.NetworkStatus
import com.typezero.sentinel.data.repository.ScanOutcome
import com.typezero.sentinel.data.repository.SentinelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatusUiState(
    val status: NetworkStatus = NetworkStatus.NORMAL,
    val scanning: Boolean = false,
    val hasScanned: Boolean = false,
    val changes: List<NetworkEvent> = emptyList(),
    val internetUp: Boolean = true,
    val gatewayUp: Boolean = true,
    val deviceCount: Int = 0,
    val onlineCount: Int = 0,
    val lastScan: Long = 0L,
    val networkKey: String? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repo: SentinelRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private val networkKey = MutableStateFlow<String?>(null)

    val devices: StateFlow<List<KnownDevice>> = networkKey
        .flatMapLatest { key ->
            if (key == null) flowOf(emptyList()) else repo.devicesForNetwork(key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<NetworkEvent>> = repo.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watched: StateFlow<List<WatchedTarget>> = repo.watchedTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scan() {
        if (_uiState.value.scanning) return
        _uiState.value = _uiState.value.copy(
            scanning = true,
            status = NetworkStatus.SCANNING,
            message = null
        )
        viewModelScope.launch {
            val outcome: ScanOutcome = repo.runScan()
            networkKey.value = outcome.networkKey
            _uiState.value = StatusUiState(
                status = outcome.status,
                scanning = false,
                hasScanned = true,
                changes = outcome.changes,
                internetUp = outcome.internetUp,
                gatewayUp = outcome.gatewayUp,
                deviceCount = outcome.deviceCount,
                onlineCount = outcome.onlineCount,
                lastScan = outcome.timestamp,
                networkKey = outcome.networkKey,
                message = if (outcome.status == NetworkStatus.NO_NETWORK)
                    "Not connected to Wi-Fi" else null
            )
        }
    }

    fun addWatched(label: String, host: String, port: Int?) {
        viewModelScope.launch { repo.addWatched(label, host, port) }
    }

    fun removeWatched(target: WatchedTarget) {
        viewModelScope.launch { repo.removeWatched(target) }
    }

    fun clearTimeline() {
        viewModelScope.launch { repo.clearTimeline() }
    }

    fun renameDevice(id: Long, name: String?) {
        viewModelScope.launch { repo.renameDevice(id, name) }
    }

    fun setDeviceType(id: Long, type: com.typezero.sentinel.data.model.DeviceType?) {
        viewModelScope.launch { repo.setDeviceType(id, type) }
    }

    fun setDeviceRoom(id: Long, room: String?) {
        viewModelScope.launch { repo.setDeviceRoom(id, room) }
    }

    class Factory(private val repo: SentinelRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(repo) as T
        }
    }
}
