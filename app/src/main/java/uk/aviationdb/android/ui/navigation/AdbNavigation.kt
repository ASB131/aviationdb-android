package uk.aviationdb.android.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalOpenMenu = staticCompositionLocalOf<() -> Unit> { {} }