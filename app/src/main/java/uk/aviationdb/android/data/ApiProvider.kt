package uk.aviationdb.android.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import uk.aviationdb.android.BuildConfig
import uk.aviationdb.android.data.api.AviationDbApi
import uk.aviationdb.android.data.credentials.SessionStore
import java.util.concurrent.TimeUnit

object ApiProvider {
    @Volatile
    private var api: AviationDbApi? = null

    @Volatile
    private var publicApi: AviationDbApi? = null

    private lateinit var sessionStore: SessionStore

    fun init(store: SessionStore) {
        sessionStore = store
    }

    fun service(): AviationDbApi {
        val cached = api
        if (cached != null) return cached
        return synchronized(this) {
            api ?: buildService(authenticated = true).also { api = it }
        }
    }

    /** Login only — no bearer header. */
    fun publicService(): AviationDbApi {
        val cached = publicApi
        if (cached != null) return cached
        return synchronized(this) {
            publicApi ?: buildService(authenticated = false).also { publicApi = it }
        }
    }

    fun reset() {
        synchronized(this) {
            api = null
        }
    }

    private fun buildService(authenticated: Boolean): AviationDbApi {
        val builders = mutableListOf<Interceptor>()
        if (authenticated) {
            builders.add(
                Interceptor { chain ->
                    val token = sessionStore.getAccessToken()
                    val request = if (!token.isNullOrBlank()) {
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $token")
                            .header("X-App-Version", BuildConfig.VERSION_NAME)
                            .build()
                    } else {
                        chain.request()
                    }
                    chain.proceed(request)
                },
            )
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        builders.add(logging)

        val client = OkHttpClient.Builder()
            .apply { builders.forEach { addInterceptor(it) } }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AviationDbApi::class.java)
    }
}