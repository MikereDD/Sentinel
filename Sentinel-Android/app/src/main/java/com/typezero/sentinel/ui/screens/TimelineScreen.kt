package com.typezero.sentinel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.typezero.sentinel.data.db.NetworkEvent
import com.typezero.sentinel.data.model.EventType
import com.typezero.sentinel.ui.theme.DarkBorder
import com.typezero.sentinel.ui.theme.SentinelAmber
import com.typezero.sentinel.ui.theme.SentinelCyan
import com.typezero.sentinel.ui.theme.SentinelGreen
import com.typezero.sentinel.ui.theme.SentinelRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ActivityFilter(val label: String) { ALL("All"), ALERTS("Alerts"), DEVICES("Devices"), INFRA("Infrastructure") }

@Composable
fun TimelineScreen(events: List<NetworkEvent>, onClear: () -> Unit, padding: PaddingValues) {
    var filter by remember { mutableStateOf(ActivityFilter.ALL) }
    val filtered = remember(events, filter) {
        events.filter { event ->
            val type = runCatching { EventType.valueOf(event.type) }.getOrNull()
            when (filter) {
                ActivityFilter.ALL -> true
                ActivityFilter.ALERTS -> type in setOf(EventType.WATCHED_OFFLINE, EventType.INTERNET_OUTAGE, EventType.GATEWAY_OFFLINE)
                ActivityFilter.DEVICES -> type in setOf(EventType.NEW_DEVICE, EventType.DEVICE_OFFLINE, EventType.DEVICE_RETURNED)
                ActivityFilter.INFRA -> type in setOf(EventType.WATCHED_OFFLINE, EventType.WATCHED_RETURNED, EventType.INTERNET_OUTAGE, EventType.INTERNET_RESTORED, EventType.GATEWAY_OFFLINE, EventType.GATEWAY_RESTORED)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Activity", style = MaterialTheme.typography.headlineLarge)
                Text("What changed on this network", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (events.isNotEmpty()) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Clear activity") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = SentinelCyan.copy(alpha = 0.16f),
                        selectedLabelColor = SentinelCyan
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        if (events.isEmpty()) "No activity yet" else "Nothing in this filter",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Run a scan and Sentinel will record meaningful changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun EventRow(event: NetworkEvent) {
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 6.dp).size(9.dp).clip(CircleShape).background(color))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (!event.detail.isNullOrBlank()) Text(event.detail!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTimestamp(event.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM d  •  HH:mm:ss", Locale.getDefault()).format(Date(ts))
