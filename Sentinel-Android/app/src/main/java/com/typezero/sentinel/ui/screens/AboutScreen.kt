package com.typezero.sentinel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.typezero.sentinel.BuildConfig
import com.typezero.sentinel.ui.theme.DarkBorder
import com.typezero.sentinel.ui.components.SentinelMark

@Composable
fun AboutScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.size(16.dp))
        SentinelMark(
            modifier = Modifier.size(112.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(16.dp))
        Text("Sentinel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Know when your network changes.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(4.dp))
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.size(24.dp))
        InfoCard(
            title = "What Sentinel does",
            body = "Sentinel sweeps the local network, checks internet and gateway health, verifies your monitors, and surfaces what changed instead of burying you in telemetry."
        )
        Spacer(Modifier.size(10.dp))
        InfoCard(
            title = "Private by design",
            body = "Scanning and history stay on-device. Sentinel has no account system, analytics, cloud service, or tracking SDK."
        )
        Spacer(Modifier.size(10.dp))
        InfoCard(
            title = "Discovery is best effort",
            body = "Android limits some network identity data, so ordinary devices use multiple discovery signals. Monitors are checked directly and are the authoritative choice for infrastructure."
        )

        Spacer(Modifier.size(22.dp))
        Text(BuildConfig.APPLICATION_ID, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DarkBorder),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
