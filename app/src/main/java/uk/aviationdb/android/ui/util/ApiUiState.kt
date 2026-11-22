package uk.aviationdb.android.ui.util

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

fun Throwable.userMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: "Request failed"
}