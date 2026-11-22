package uk.aviationdb.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.screens.LoginScreen

@Composable
fun AviationDbRoot(sessionStore: SessionStore) {
    var signedIn by rememberSaveable { mutableStateOf(sessionStore.isSignedIn()) }

    if (!signedIn) {
        LoginScreen(
            sessionStore = sessionStore,
            onSignedIn = { signedIn = true },
        )
    } else {
        AviationDbApp(
            sessionStore = sessionStore,
            onSignedOut = {
                signedIn = false
            },
        )
    }
}