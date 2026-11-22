package uk.aviationdb.android

import android.app.Application
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.ApiKeyStore

class AviationDbApplication : Application() {
    lateinit var apiKeyStore: ApiKeyStore
        private set

    override fun onCreate() {
        super.onCreate()
        apiKeyStore = ApiKeyStore(this)
        ApiProvider.init(apiKeyStore)
    }
}