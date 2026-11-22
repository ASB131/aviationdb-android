package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.SessionStore

@Composable
fun ExploreScreen(sessionStore: SessionStore) {
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ExploreMode.Aircraft) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val signedIn = sessionStore.isSignedIn()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Explore", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Look up aircraft and airlines using the same API as the website.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (!signedIn) {
            Text("Please sign in to search.", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it.uppercase() },
            label = { Text(if (mode == ExploreMode.Aircraft) "Registration or ICAO24" else "ICAO / IATA / name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = {
                mode = ExploreMode.Aircraft
                scope.launch { search(mode, query, { busy = it }, { result = it }, { error = it }) }
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = query.length >= 2 && !busy,
        ) { Text("Search aircraft") }

        Button(
            onClick = {
                mode = ExploreMode.Airline
                scope.launch { search(mode, query, { busy = it }, { result = it }, { error = it }) }
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = query.length >= 2 && !busy,
        ) { Text("Search airline") }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }
        result?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private enum class ExploreMode { Aircraft, Airline }

private suspend fun search(
    mode: ExploreMode,
    query: String,
    setBusy: (Boolean) -> Unit,
    setResult: (String?) -> Unit,
    setError: (String?) -> Unit,
) {
    setBusy(true)
    setError(null)
    setResult(null)
    try {
        val api = ApiProvider.service()
        val map = when (mode) {
            ExploreMode.Aircraft -> api.aircraftShort(query.trim())
            ExploreMode.Airline -> api.airlineShort(query.trim())
        }
        setResult(map.entries.joinToString("\n") { (k, v) -> "$k: $v" }.ifBlank { "No data returned." })
    } catch (e: HttpException) {
        setError(when (e.code()) {
            401 -> "Invalid API key."
            404 -> "Not found."
            else -> "HTTP ${e.code()}"
        })
    } catch (e: Exception) {
        setError(e.message ?: "Search failed")
    } finally {
        setBusy(false)
    }
}