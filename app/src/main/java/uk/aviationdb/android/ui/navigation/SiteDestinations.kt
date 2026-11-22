package uk.aviationdb.android.ui.navigation

/** Remaining in-app WebView pages (no duplicate of native Browse / Stats / Account / Live). */
data class SiteDestination(
    val id: String,
    val title: String,
    val path: String,
)

object SiteDestinations {
    const val ROUTE_PREFIX = "site"

    fun routeFor(id: String): String = "$ROUTE_PREFIX/$id"

    val menu: List<SiteDestination> = listOf(
        SiteDestination("community", "Community", "/community"),
        SiteDestination("news", "News", "/news"),
        SiteDestination("pricing", "Pricing", "/pricing"),
    )

    val all: List<SiteDestination> = menu + listOf(
        SiteDestination("support", "Support", "/support"),
    )

    fun byId(id: String): SiteDestination? = all.firstOrNull { it.id == id }
}