package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.ProfileResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.theme.Danger

@Composable
fun AccountScreen(
    sessionStore: SessionStore,
    onSignedOut: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        try {
            profile = ApiProvider.service().profile()
            profile?.let { p ->
                sessionStore.updateProfile(
                    email = p.user.email,
                    displayName = p.user.displayName,
                    hasTracking = p.plan.hasTracking == true,
                    trackLat = p.user.trackLat,
                    trackLon = p.user.trackLon,
                )
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AdbTopBanner(
            title = (profile?.user?.displayName ?: sessionStore.getDisplayName().orEmpty()).ifBlank { "Account" },
            subtitle = profile?.user?.email ?: sessionStore.getUserEmail(),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                profile != null -> {
                    val p = profile!!
                    AdbCard {
                        Text("Plan", style = MaterialTheme.typography.titleMedium)
                        Text(p.plan.name ?: "—", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${p.plan.priceLabel ?: ""} · ${p.plan.historyDays ?: 0} day history",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            if (p.plan.hasTracking == true) "Tracking active" else "No tracking plan",
                            color = if (p.plan.hasTracking == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    AdbCard(modifier = Modifier.padding(top = 10.dp)) {
                        Text("Tracking location", style = MaterialTheme.typography.titleMedium)
                        val lat = p.user.trackLat
                        val lon = p.user.trackLon
                        Text(
                            if (lat != null && lon != null) {
                                "Set: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                            } else {
                                "Not set — configure on aviationdb.uk → Account → Tracking"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("Timezone: ${p.user.timezone ?: "—"}", style = MaterialTheme.typography.bodySmall)
                    }

                    AdbCard(modifier = Modifier.padding(top = 10.dp)) {
                        Text("Billing", style = MaterialTheme.typography.titleMedium)
                        Text("Stripe: ${p.billing.stripeStatus ?: "—"}", style = MaterialTheme.typography.bodySmall)
                        p.billing.currentPeriodEnd?.let {
                            Text("Renews / ends: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        p.billing.recentInvoices.orEmpty().take(5).forEach { inv ->
                            val gbp = (inv.amountPaid ?: 0) / 100.0
                            Text(
                                "£${"%.2f".format(gbp)} · ${inv.status} · ${inv.createdAt}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Text(
                            "Manage subscription and payment methods on the website.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val sess = ApiProvider.service().session()
                            sessionStore.updateProfile(
                                email = sess.user.email,
                                displayName = sess.user.displayName,
                                hasTracking = sess.user.hasTracking == true,
                                trackLat = sess.user.trackLat,
                                trackLon = sess.user.trackLon,
                            )
                            profile = ApiProvider.service().profile()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) { Text("Refresh") }

            Button(
                onClick = {
                    scope.launch {
                        runCatching { ApiProvider.service().logout() }
                        sessionStore.clear()
                        ApiProvider.reset()
                        onSignedOut()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) { Text("Sign out") }
        }
    }
}