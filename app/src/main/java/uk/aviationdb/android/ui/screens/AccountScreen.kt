package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.ProfileResponse
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbCard
import uk.aviationdb.android.ui.components.AdbDetailRow
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
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

    Column(modifier = Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = (profile?.user?.displayName ?: sessionStore.getDisplayName().orEmpty()).ifBlank { "Account" },
            subtitle = profile?.user?.email ?: sessionStore.getUserEmail(),
            onMenuClick = LocalOpenMenu.current,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                profile != null -> {
                    val p = profile!!
                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Subscription", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            AdbDetailRow("Plan", p.plan.name ?: "—")
                            AdbDetailRow("Price", p.plan.priceLabel ?: "—")
                            AdbDetailRow("History", "${p.plan.historyDays ?: 0} days")
                            Text(
                                if (p.plan.hasTracking == true) "Tracking active" else "No tracking plan",
                                color = if (p.plan.hasTracking == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tracking", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            val lat = p.user.trackLat
                            val lon = p.user.trackLon
                            AdbDetailRow(
                                "Home coordinates",
                                if (lat != null && lon != null) {
                                    "${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                                } else {
                                    "Not set"
                                },
                            )
                            AdbDetailRow("Timezone", p.user.timezone ?: "—")
                            Text(
                                "Update tracking location on aviationdb.uk → Account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Profile", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            AdbDetailRow("Email", p.user.email)
                            p.user.status?.let { AdbDetailRow("Account status", it) }
                            p.user.memberSince?.let { AdbDetailRow("Member since", it) }
                        }
                    }

                    AdbCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Billing", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            AdbDetailRow("Stripe status", p.billing.stripeStatus ?: "—")
                            p.billing.currentPeriodEnd?.let { AdbDetailRow("Period ends", it) }
                            if (p.billing.cancelAtPeriodEnd == true) {
                                Text(
                                    "Cancels at end of current period",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            val invoices = p.billing.recentInvoices.orEmpty().take(5)
                            if (invoices.isEmpty()) {
                                Text("No invoices on file.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Recent invoices", style = MaterialTheme.typography.labelMedium)
                                invoices.forEach { inv ->
                                    val gbp = (inv.amountPaid ?: 0) / 100.0
                                    val line = buildString {
                                        append("£${"%.2f".format(gbp)}")
                                        inv.status?.let { append(" · $it") }
                                        inv.createdAt?.let { append(" · $it") }
                                    }
                                    Text(
                                        line,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                "Manage payment methods on the website.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                            error = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
            ) { Text("Sign out") }
        }
    }
}