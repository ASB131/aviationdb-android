package uk.aviationdb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uk.aviationdb.android.ui.AviationDbRoot
import uk.aviationdb.android.ui.theme.AviationDbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AviationDbApplication
        enableEdgeToEdge()
        setContent {
            AviationDbTheme {
                AviationDbRoot(sessionStore = app.sessionStore)
            }
        }
    }
}