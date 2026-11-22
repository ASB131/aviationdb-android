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
import uk.aviationdb.android.data.credentials.ApiKeyStore
import java.util.concurrent.TimeUnit

object ApiProvider {
  @Volatile
    private var api: AviationDbApi? = null

    private lateinit var keyStore: ApiKeyStore

    fun init(store: ApiKeyStore) {
        keyStore = store
    }

    fun service(): AviationDbApi {
        val cached = api
        if (cached != null) return cached
        return synchronized(this) {
            api ?: buildService().also { api = it }
        }
    }

    fun reset() {
        synchronized(this) {
            api = null
        }
    }

    private fun buildService(): AviationDbApi {
        val auth = Interceptor { chain ->
            val key = keyStore.getApiKey()
            val request = if (!key.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $key")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
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