package uk.aviationdb.android.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uk.aviationdb.android.BuildConfig
import uk.aviationdb.android.data.credentials.SessionStore
import uk.aviationdb.android.ui.components.AdbTopBanner
import uk.aviationdb.android.ui.navigation.LocalOpenMenu
import uk.aviationdb.android.ui.navigation.openSitePath
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteWebScreen(
    sessionStore: SessionStore,
    sitePath: String,
    title: String,
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val safePath = remember(sitePath) {
        val p = sitePath.trim().let { if (it.startsWith("/")) it else "/$it" }
        if (p.contains("://")) "/home" else p
    }
    val token = sessionStore.getAccessToken()
    var loading by remember(safePath) { mutableStateOf(true) }
    var pageTitle by remember(safePath) { mutableStateOf(title) }
    var error by remember { mutableStateOf<String?>(null) }

    val launchUrl = remember(safePath, token) {
        val base = BuildConfig.SITE_BASE_URL.trimEnd('/')
        val redirect = URLEncoder.encode(safePath, Charsets.UTF_8.name())
        "$base/app_sso.php?redirect=$redirect"
    }

    Column(Modifier.fillMaxSize()) {
        AdbTopBanner(
            title = pageTitle,
            subtitle = "AviationDB.uk",
            onBackClick = onBack,
            onMenuClick = LocalOpenMenu.current,
        )
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
        if (token.isNullOrBlank()) {
            Text(
                "Sign in on the app first to load this page with your account.",
                modifier = Modifier.padding(16.dp),
            )
            CircularProgressIndicator(Modifier.padding(24.dp))
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                view?.title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val uri = request?.url ?: return false
                                val host = uri.host?.lowercase() ?: return false
                                val allowed = host == "aviationdb.uk" || host == "www.aviationdb.uk"
                                if (!allowed) {
                                    openSitePath(context, uri.toString())
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(
                            launchUrl,
                            mapOf(
                                "Authorization" to "Bearer $token",
                                "X-App-Version" to BuildConfig.VERSION_NAME,
                            ),
                        )
                    }
                },
                update = { view ->
                    if (view.url.isNullOrBlank()) {
                        view.loadUrl(
                            launchUrl,
                            mapOf(
                                "Authorization" to "Bearer $token",
                                "X-App-Version" to BuildConfig.VERSION_NAME,
                            ),
                        )
                    }
                },
            )
        }
    }
}