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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.ApiKeyStore

@Composable
fun AccountScreen(apiKeyStore: ApiKeyStore) {
    var draft by remember { mutableStateOf(apiKeyStore.getApiKey().orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Account", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Create an API key at aviationdb.uk → Account. The key is stored encrypted on this device only.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("API key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        Button(
            onClick = {
                apiKeyStore.setApiKey(draft)
                ApiProvider.reset()
                status = "Saved."
                error = null
            },
            modifier = Modifier.padding(top = 8.dp),
            enabled = draft.isNotBlank(),
        ) { Text("Save key") }

        TextButton(
            onClick = {
                apiKeyStore.clearApiKey()
                draft = ""
                ApiProvider.reset()
                status = "Key removed."
                error = null
            },
        ) { Text("Clear key") }

        Button(
            onClick = {
                scope.launch {
                    error = null
                    status = null
                    try {
                        val summary = ApiProvider.service().summary()
                        status = buildString {
                            append("Connected. ")
                            summary.activeNow?.let { append("$it overhead now. ") }
                            summary.totalSightings?.let { append("$it sightings in history window.") }
                        }.trim()
                    } catch (e: HttpException) {
                        error = when (e.code()) {
                            401, 403 -> "Key invalid or tracking subscription required."
                            else -> "HTTP ${e.code()}"
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Test failed"
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
            enabled = draft.isNotBlank(),
        ) { Text("Test connection") }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
}