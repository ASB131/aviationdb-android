package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.SessionStore

@Composable
fun AccountScreen(
    sessionStore: SessionStore,
    onSignedOut: () -> Unit,
) {
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val email = sessionStore.getUserEmail().orEmpty()
    val name = sessionStore.getDisplayName().orEmpty()
    val tracking = sessionStore.hasTracking()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Account", style = MaterialTheme.typography.headlineSmall)
        if (name.isNotBlank()) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        Text(email, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        Text(
            if (tracking) "Tracking plan active" else "No active tracking plan — Live and Sightings need a tracking subscription",
            style = MaterialTheme.typography.bodySmall,
            color = if (tracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    status = null
                    try {
                        val summary = ApiProvider.service().summary()
                        status = buildString {
                            append("Connected. ")
                            summary.activeNow?.let { append("$it overhead now. ") }
                            summary.totalSightings?.let { append("$it sightings in history window.") }
                        }.trim()
                    } catch (e: Exception) {
                        error = e.message ?: "Could not reach server"
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) { Text("Refresh status") }

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    try {
                        runCatching { ApiProvider.service().logout() }
                    } finally {
                        sessionStore.clear()
                        ApiProvider.reset()
                        busy = false
                        onSignedOut()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            enabled = !busy,
        ) { Text("Sign out") }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
}