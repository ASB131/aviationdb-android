package uk.aviationdb.android.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import uk.aviationdb.android.BuildConfig

fun openSitePath(context: Context, path: String) {
    val base = BuildConfig.SITE_BASE_URL.trimEnd('/')
    val url = if (path.startsWith("http")) path else base + if (path.startsWith("/")) path else "/$path"
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}