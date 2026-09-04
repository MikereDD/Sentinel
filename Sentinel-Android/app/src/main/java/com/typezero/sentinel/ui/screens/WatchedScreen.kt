package com.typezero.sentinel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.typezero.sentinel.data.db.WatchedTarget
import com.typezero.sentinel.ui.theme.DarkBorder
import com.typezero.sentinel.ui.theme.SentinelGreen
import com.typezero.sentinel.ui.theme.SentinelRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchedScreen(
    targets: List<WatchedTarget>,
    onAdd: (String, String, Int?) -> Unit,
    onRemove: (WatchedTarget) -> Unit,
    padding: PaddingValues
) {
    var adding by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Monitors", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.size(4.dp))
                Text(
                    "Check important devices and services directly, without relying on discovery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(14.dp))
                Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add monitor")
                }
            }
        }

        if (targets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Text("Nothing monitored yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add the router, a server, Home Assistant, or any TCP service you always want verified.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(targets, key = { it.id }) { target -> WatchedRow(target, onRemove) }
        }
    }

    if (adding) {
        AddMonitorDialog(
            onDismiss = { adding = false },
            onAdd = { label, host, port ->
                onAdd(label, host, port)
                adding = false
            }
        )
    }
}

@Composable
private fun AddMonitorDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int?) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add monitor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Leave the port blank for reachability, or enter a TCP port to verify a specific service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Name") }, placeholder = { Text("Arakiel") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host or IP") }, placeholder = { Text("192.168.1.10") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit) }, label = { Text("TCP port (optional)") }, placeholder = { Text("22") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && host.isNotBlank(),
                onClick = { onAdd(label.trim(), host.trim(), port.toIntOrNull()) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WatchedRow(target: WatchedTarget, onRemove: (WatchedTarget) -> Unit) {
    val statusColor = when (target.lastOnline) {
        true -> SentinelGreen
        false -> SentinelRed
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(target.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    target.host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    target.port?.let { "TCP :$it" } ?: "Reachability",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when (target.lastOnline) { true -> "Online"; false -> "Unavailable"; null -> "Not checked yet" },
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
                if (target.lastChecked > 0L) {
                    Text(
                        "Last checked ${formatMonitorTime(target.lastChecked)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onRemove(target) }) { Icon(Icons.Filled.Close, contentDescription = "Remove monitor") }
        }
    }
}
private fun formatMonitorTime(ts: Long): String =
    SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(ts))
