package uk.aviationdb.android

import android.app.Application
import uk.aviationdb.android.data.ApiProvider
import uk.aviationdb.android.data.credentials.SessionStore

class AviationDbApplication : Application() {
    lateinit var sessionStore: SessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        ApiProvider.init(sessionStore)
    }
}