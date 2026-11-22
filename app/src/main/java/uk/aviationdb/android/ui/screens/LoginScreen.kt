package uk.aviationdb.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uk.aviationdb.android.BuildConfig
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.api.ApiErrorEnvelope
import uk.aviationdb.android.data.api.LoginRequest
import uk.aviationdb.android.data.credentials.SessionStore

@Composable
fun LoginScreen(
    sessionStore: SessionStore,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf(sessionStore.getUserEmail().orEmpty()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val errorAdapter = remember {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ApiErrorEnvelope::class.java)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("AviationDB", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Sign in with the same email and password you use on aviationdb.uk",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { rememberMe = !rememberMe },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Stay signed in for 30 days")
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val res = ApiProvider.publicService().login(
                            LoginRequest(
                                email = email,
                                password = password,
                                remember = rememberMe,
                                appVersion = BuildConfig.VERSION_NAME,
                            ),
                        )
                        sessionStore.saveSession(
                            token = res.accessToken,
                            email = res.user.email,
                            displayName = res.user.displayName,
                            hasTracking = res.user.hasTracking == true,
                        )
                        ApiProvider.reset()
                        onSignedIn()
                    } catch (e: HttpException) {
                        val body = e.response()?.errorBody()?.string()
                        val apiMsg = body?.let {
                            try {
                                errorAdapter.fromJson(it)?.error?.message
                            } catch (_: Exception) {
                                null
                            }
                        }
                        error = apiMsg ?: when (e.code()) {
                            401 -> "Email or password is incorrect."
                            403 -> "Account cannot sign in yet (check verification or subscription)."
                            429 -> "Too many attempts. Wait a few minutes."
                            else -> "Sign-in failed (HTTP ${e.code()})."
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Sign-in failed"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Signing in…" else "Sign in")
        }
    }
}