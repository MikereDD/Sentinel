package com.typezero.sentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.typezero.sentinel.ui.MainViewModel
import com.typezero.sentinel.ui.screens.AboutScreen
import com.typezero.sentinel.ui.screens.StatusScreen
import com.typezero.sentinel.ui.screens.TimelineScreen
import com.typezero.sentinel.ui.screens.WatchedScreen
import com.typezero.sentinel.ui.theme.DarkSurface
import com.typezero.sentinel.ui.theme.SentinelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as SentinelApp).repository
        setContent {
            SentinelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SentinelApp(viewModel(factory = MainViewModel.Factory(repo)))
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    STATUS("Status", Icons.Filled.Shield),
    ACTIVITY("Activity", Icons.Filled.History),
    MONITORS("Monitors", Icons.Filled.Visibility),
    ABOUT("About", Icons.Filled.Info)
}

@Composable
private fun SentinelApp(vm: MainViewModel) {
    var tab by remember { mutableStateOf(Tab.STATUS) }

    val uiState by vm.uiState.collectAsState()
    val devices by vm.devices.collectAsState()
    val events by vm.events.collectAsState()
    val watched by vm.watched.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.STATUS -> StatusScreen(
                state = uiState,
                devices = devices,
                onScan = vm::scan,
                onRename = vm::renameDevice,
                onSetType = vm::setDeviceType,
                onSetRoom = vm::setDeviceRoom,
                padding = padding
            )
            Tab.ACTIVITY -> TimelineScreen(events = events, onClear = vm::clearTimeline, padding = padding)
            Tab.MONITORS -> WatchedScreen(targets = watched, onAdd = vm::addWatched, onRemove = vm::removeWatched, padding = padding)
            Tab.ABOUT -> AboutScreen(padding = padding)
        }
    }
}
