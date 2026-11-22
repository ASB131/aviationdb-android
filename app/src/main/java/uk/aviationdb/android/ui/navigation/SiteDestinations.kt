package uk.aviationdb.android.ui.navigation

/**
 * In-app WebView destinations mirroring aviationdb.uk site nav.
 */
data class SiteDestination(
    val id: String,
    val title: String,
    val path: String,
)

object SiteDestinations {
    const val ROUTE_PREFIX = "site"

    fun routeFor(id: String): String = "$ROUTE_PREFIX/$id"

    val all: List<SiteDestination> = listOf(
        SiteDestination("home", "Home", "/home"),
        SiteDestination("stats_full", "Full stats & charts", "/stats_full"),
        SiteDestination("stats", "Stats dashboard (web)", "/stats"),
        SiteDestination("active", "Active map (web)", "/active"),
        SiteDestination("kiosk", "Kiosk", "/kiosk"),
        SiteDestination("search", "Aircraft search", "/search"),
        SiteDestination("callsigns", "Callsigns", "/callsigns"),
        SiteDestination("airlines", "Airlines", "/airlines"),
        SiteDestination("airports", "Airports", "/airports"),
        SiteDestination("countries", "Countries", "/country"),
        SiteDestination("models", "Aircraft models", "/production_list"),
        SiteDestination("fir", "FIR map", "/fir"),
        SiteDestination("community", "Community", "/community"),
        SiteDestination("news", "News", "/news"),
        SiteDestination("pricing", "Pricing", "/pricing"),
        SiteDestination("account", "Account & billing", "/account"),
        SiteDestination("support", "Support", "/support"),
        SiteDestination("api", "API docs", "/api/docs"),
        SiteDestination("about", "About", "/about"),
    )

    fun byId(id: String): SiteDestination? = all.firstOrNull { it.id == id }
}