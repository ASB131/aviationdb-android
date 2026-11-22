package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.theme.Danger

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
            .verticalScroll(rememberScrollState()),
    ) {
        AdbTopBanner(
            title = name.ifBlank { "Account" },
            subtitle = email,
        )

        Column(modifier = Modifier.padding(16.dp)) {
            AdbCard {
                Text(
                    if (tracking) "Tracking plan active" else "No tracking plan on this account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (tracking) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
                Text(
                    "Subscriptions, tracking location, API keys, and data export are managed on aviationdb.uk → Account.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        status = null
                        try {
                            val sess = ApiProvider.service().session()
                            sessionStore.updateProfile(
                                email = sess.user.email,
                                displayName = sess.user.displayName,
                                hasTracking = sess.user.hasTracking == true,
                                trackLat = sess.user.trackLat,
                                trackLon = sess.user.trackLon,
                            )
                            status = buildString {
                                append("Connected")
                                sess.user.planName?.let { append(" · $it") }
                            }.trim()
                        } catch (e: Exception) {
                            error = e.message ?: "Could not reach server"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = !busy,
            ) { Text("Refresh from server") }

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
                    .padding(top = 8.dp),
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) { Text("Sign out") }

            status?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}