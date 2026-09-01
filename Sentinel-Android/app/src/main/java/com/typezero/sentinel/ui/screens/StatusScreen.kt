package com.typezero.sentinel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.typezero.sentinel.data.db.KnownDevice
import com.typezero.sentinel.data.db.NetworkEvent
import com.typezero.sentinel.data.model.DeviceType
import com.typezero.sentinel.data.model.EventType
import com.typezero.sentinel.data.model.NetworkStatus
import com.typezero.sentinel.ui.StatusUiState
import com.typezero.sentinel.ui.components.SentinelMark
import com.typezero.sentinel.ui.theme.DarkBorder
import com.typezero.sentinel.ui.theme.DarkSurfaceRaised
import com.typezero.sentinel.ui.theme.SentinelAmber
import com.typezero.sentinel.ui.theme.SentinelCyan
import com.typezero.sentinel.ui.theme.SentinelGreen
import com.typezero.sentinel.ui.theme.SentinelRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(
    state: StatusUiState,
    devices: List<KnownDevice>,
    onScan: () -> Unit,
    onRename: (Long, String?) -> Unit,
    onSetType: (Long, DeviceType?) -> Unit,
    onSetRoom: (Long, String?) -> Unit,
    padding: PaddingValues
) {
    var editing by remember { mutableStateOf<KnownDevice?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StatusCard(state, onScan) }

        if (state.hasScanned && state.status != NetworkStatus.NO_NETWORK) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfraChip("Internet", state.internetUp, Icons.Filled.Public, Modifier.weight(1f))
                    InfraChip("Gateway", state.gatewayUp, Icons.Filled.Router, Modifier.weight(1f))
                }
            }
        }

        if (state.changes.isNotEmpty()) {
            item { SectionHeading("Changes", "${state.changes.size} this scan") }
            items(state.changes) { change ->
                ChangeRow(change)
            }
        } else if (state.hasScanned && !state.scanning && state.status == NetworkStatus.NORMAL) {
            item {
                QuietStateCard(
                    title = "No important changes",
                    subtitle = "Sentinel found nothing new to report."
                )
            }
        }

        if (devices.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Devices",
                    meta = "${state.onlineCount} active  •  ${state.deviceCount} known"
                )
            }

            val byRoom = devices.groupBy { it.room?.takeIf { r -> r.isNotBlank() } }
            if (byRoom.keys.none { it != null }) {
                items(devices, key = { it.id }) { device -> DeviceRow(device) { editing = device } }
            } else {
                byRoom.keys.filterNotNull().sortedBy { it.lowercase() }.forEach { room ->
                    item(key = "room:$room") { RoomHeader(room) }
                    items(byRoom.getValue(room), key = { it.id }) { device -> DeviceRow(device) { editing = device } }
                }
                byRoom[null]?.let { unassigned ->
                    item(key = "room:unassigned") { RoomHeader("Unassigned") }
                    items(unassigned, key = { it.id }) { device -> DeviceRow(device) { editing = device } }
                }
            }
        }
    }

    editing?.let { device ->
        val existingRooms = remember(devices) {
            devices.mapNotNull { it.room?.takeIf { r -> r.isNotBlank() } }
                .distinct().sortedBy { it.lowercase() }
        }
        EditDeviceDialog(
            device = device,
            knownRooms = existingRooms,
            onSave = { name, type, room ->
                onRename(device.id, name)
                onSetType(device.id, type)
                onSetRoom(device.id, room)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun SectionHeading(title: String, meta: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        meta?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoomHeader(name: String) {
    Text(
        name.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun StatusCard(state: StatusUiState, onScan: () -> Unit) {
    val visual = when {
        state.scanning -> StatusVisual("Scanning", "Checking devices and services", SentinelCyan, Icons.Filled.Refresh)
        state.status == NetworkStatus.NO_NETWORK -> StatusVisual("No network", state.message ?: "Connect to Wi-Fi", SentinelRed, Icons.Filled.Warning)
        !state.hasScanned -> StatusVisual("Ready", "Check this network for changes", SentinelCyan, Icons.Filled.CheckCircle)
        state.status == NetworkStatus.DEGRADED -> StatusVisual("Needs attention", "Infrastructure or a monitor is unavailable", SentinelAmber, Icons.Filled.Warning)
        state.status == NetworkStatus.CHANGED -> StatusVisual("Changes detected", "${state.changes.size} event${if (state.changes.size == 1) "" else "s"} this scan", SentinelCyan, Icons.Filled.Warning)
        else -> StatusVisual("All clear", "No important changes", SentinelGreen, Icons.Filled.CheckCircle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, DarkBorder),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceRaised)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(visual.color.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    SentinelMark(modifier = Modifier.size(38.dp), color = visual.color)
                }
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("NETWORK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(visual.label, style = MaterialTheme.typography.headlineMedium, color = visual.color)
                    Text(visual.sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.networkKey != null || state.lastScan > 0L) {
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    state.networkKey?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    } ?: Spacer(Modifier.weight(1f))
                    if (state.lastScan > 0L) {
                        Text("Last scan ${formatTime(state.lastScan)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onScan,
                enabled = !state.scanning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text("Scanning…")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Scan now")
                }
            }
        }
    }
}

private data class StatusVisual(
    val label: String,
    val sub: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun InfraChip(
    label: String,
    up: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val color = if (up) SentinelGreen else SentinelRed
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (up) "Online" else "Unavailable", color = color, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun QuietStateCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(SentinelGreen))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChangeRow(event: NetworkEvent) {
    val type = runCatching { EventType.valueOf(event.type) }.getOrNull()
    val color = when (type) {
        EventType.NEW_DEVICE -> SentinelCyan
        EventType.DEVICE_RETURNED, EventType.WATCHED_RETURNED, EventType.INTERNET_RESTORED, EventType.GATEWAY_RESTORED -> SentinelGreen
        EventType.DEVICE_OFFLINE -> SentinelAmber
        EventType.WATCHED_OFFLINE, EventType.INTERNET_OUTAGE, EventType.GATEWAY_OFFLINE -> SentinelRed
        else -> SentinelAmber
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (!event.detail.isNullOrBlank()) {
                    Text(event.detail!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(formatClock(event.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeviceRow(device: KnownDevice, onClick: () -> Unit) {
    val autoType = runCatching { DeviceType.valueOf(device.deviceType) }.getOrDefault(DeviceType.UNKNOWN)
    val type = device.userType?.let { runCatching { DeviceType.valueOf(it) }.getOrNull() } ?: autoType
    val name = friendlyDeviceName(device, type)

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(
                (if (device.online) SentinelGreen else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.09f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                typeIcon(type),
                contentDescription = type.pretty,
                tint = if (device.online) SentinelGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        append(device.lastIp)
                        if (type != DeviceType.UNKNOWN && type != DeviceType.ROUTER) append("  •  ${type.pretty}")
                    },
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!device.strongIdentity) {
                    Spacer(Modifier.size(8.dp))
                    WeakIdentityBadge()
                }
            }
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (device.online) SentinelGreen else MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun WeakIdentityBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SentinelAmber.copy(alpha = 0.11f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            "WEAK ID",
            style = MaterialTheme.typography.labelMedium,
            color = SentinelAmber,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun friendlyDeviceName(device: KnownDevice, type: DeviceType): String {
    device.customName?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

    if (type == DeviceType.ROUTER || device.lastIp == device.networkKey) return "Router / Gateway"

    var raw = device.displayName.trim()
    if (raw == device.lastIp || raw.isBlank()) {
        return when (type) {
            DeviceType.TV -> "Smart TV"
            DeviceType.PHONE -> "Phone / Tablet"
            DeviceType.COMPUTER -> "Computer"
            DeviceType.NAS -> "NAS / Storage"
            DeviceType.PRINTER -> "Printer"
            DeviceType.IOT -> "Smart home device"
            DeviceType.SPEAKER -> "Speaker"
            DeviceType.CAMERA -> "Camera"
            DeviceType.SERVER -> "Server"
            DeviceType.GAME_CONSOLE -> "Game console"
            else -> "Unknown device"
        }
    }

    // Common generated hostnames are useful evidence but poor primary labels.
    val atPrefix = Regex("^[0-9A-Fa-f]{8,}@(.+)$").matchEntire(raw)
    if (atPrefix != null) raw = atPrefix.groupValues[1]

    if (Regex("(?i)^smart[-_ ]?tv[-_][0-9a-f]{12,}$").matches(raw)) return "Smart TV"

    val strippedHexTail = raw.replace(Regex("(?i)[-_][0-9a-f]{12,}$"), "")
    if (strippedHexTail != raw && strippedHexTail.length >= 3) {
        raw = strippedHexTail.replace('-', ' ').replace('_', ' ').trim()
    }

    return raw
}

private fun typeIcon(type: DeviceType): androidx.compose.ui.graphics.vector.ImageVector =
    when (type) {
        DeviceType.PHONE -> Icons.Filled.Smartphone
        DeviceType.COMPUTER -> Icons.Filled.Computer
        DeviceType.TV -> Icons.Filled.Tv
        DeviceType.ROUTER -> Icons.Filled.Router
        DeviceType.NAS -> Icons.Filled.Storage
        DeviceType.PRINTER -> Icons.Filled.Print
        DeviceType.IOT -> Icons.Filled.Sensors
        DeviceType.SPEAKER -> Icons.Filled.Speaker
        DeviceType.CAMERA -> Icons.Filled.Videocam
        DeviceType.SERVER -> Icons.Filled.Dns
        DeviceType.GAME_CONSOLE -> Icons.Filled.VideogameAsset
        DeviceType.UNKNOWN -> Icons.Filled.DeviceUnknown
    }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditDeviceDialog(
    device: KnownDevice,
    knownRooms: List<String>,
    onSave: (name: String?, type: DeviceType?, room: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(device.customName ?: device.displayName) }
    var selectedType by remember {
        mutableStateOf(
            device.userType?.let { runCatching { DeviceType.valueOf(it) }.getOrNull() }
        )
    }
    var room by remember { mutableStateOf(device.room ?: "") }
    val typeOptions = remember { DeviceType.entries.filter { it != DeviceType.UNKNOWN } }
    val roomSuggestions = remember(knownRooms) {
        val presets = listOf(
            "Living room", "Bedroom", "Kitchen", "Office",
            "Patio", "Garage", "Bathroom", "Outside"
        )
        (knownRooms + presets).distinctBy { it.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit device") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    device.lastIp,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("Auto") }
                    )
                    typeOptions.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.pretty) }
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text("Room", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(4.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room") },
                    singleLine = true,
                    placeholder = { Text("Unassigned") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roomSuggestions.forEach { r ->
                        FilterChip(
                            selected = room.trim().equals(r, ignoreCase = true),
                            onClick = {
                                room = if (room.trim().equals(r, ignoreCase = true)) "" else r
                            },
                            label = { Text(r) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                val nameArg = when {
                    trimmed.isBlank() -> null
                    trimmed == device.displayName && device.customName == null -> null
                    else -> trimmed
                }
                onSave(nameArg, selectedType, room.trim().takeIf { it.isNotBlank() })
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(ts))


private fun formatClock(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
