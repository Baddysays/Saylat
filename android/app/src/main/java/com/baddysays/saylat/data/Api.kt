package com.baddysays.saylat.data

import com.baddysays.saylat.network.RetryInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SaylatApi {
    @GET("api/extract")
    suspend fun extract(
        @Query("url") url: String,
        @Query("images") images: String = "normal",
        @Query("level") level: String = "medium",
    ): SaylatArticle

    @GET("api/render/visual")
    suspend fun renderVisual(
        @Query("url") url: String,
        @Query("images") images: String = "tiny",
    ): VisualPage

    @GET("api/render/strips")
    suspend fun renderStrips(
        @Query("url") url: String,
        @Query("images") images: String = "tiny",
        @Query("engine") engine: String = "browser",
    ): StripPage

    @POST("api/open")
    suspend fun open(@Body body: OpenRequest): OpenResponse

    @GET("api/connect/status")
    suspend fun connectStatus(): ConnectStatus

    @POST("api/connect/telegram/code")
    suspend fun telegramCode(@Body body: TelegramCodeRequest): MessageResponse

    @POST("api/connect/telegram/signin")
    suspend fun telegramSignIn(@Body body: TelegramSignInRequest): MessageResponse

    @GET("api/connect/credentials")
    suspend fun getServiceCredentials(): ServiceCredentialsPublic

    @retrofit2.http.PUT("api/connect/credentials")
    suspend fun putServiceCredentials(@Body body: ServiceCredentialsUpdate): ServiceCredentialsPublic

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("engine") engine: String = "searxng",
    ): ProxySearchResponse

    @GET("api/app/update")
    suspend fun appUpdate(): AppUpdateInfo

    @POST("api/translate")
    suspend fun translate(@Body body: TranslateRequest): TranslateResponse

    @POST("api/act")
    suspend fun act(@Body body: ActRequest): ActResponse

    @GET("api/feed")
    suspend fun unifiedFeed(
        @Query("limit") limit: Int = 12,
        @Query("offset") offset: Int = 0,
        @Query("page_size") pageSize: Int = 24,
    ): SaylatFeed
}

object ApiFactory {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private data class ClientKey(
        val baseUrl: String,
        val slowNetwork: Boolean,
        val compressionLevel: String,
        val apiKey: String,
    )

    @Volatile
    private var cached: Pair<ClientKey, SaylatApi>? = null

    fun create(
        baseUrl: String,
        slowNetwork: Boolean = false,
        compressionLevel: String = CompressionLevel.MEDIUM,
        apiKey: String = "",
    ): SaylatApi {
        val key = ClientKey(
            baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/",
            slowNetwork = slowNetwork,
            compressionLevel = compressionLevel,
            apiKey = apiKey.trim(),
        )
        synchronized(this) {
            cached?.let { (existingKey, api) ->
                if (existingKey == key) return api
            }
        }
        val api = buildClient(key)
        synchronized(this) {
            cached = key to api
        }
        return api
    }

    fun invalidateCache() {
        synchronized(this) {
            cached = null
        }
    }

    private fun buildClient(key: ClientKey): SaylatApi {
        val connectSec = if (key.slowNetwork) 60L else 30L
        val readSec = if (key.slowNetwork) 180L else 90L
        // Server reads this in main.py (_extract_safe) when ?level= is omitted.
        val levelHeader = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Saylat-Level", key.compressionLevel)
                    .build(),
            )
        }
        val apiKeyInterceptor = if (key.apiKey.isEmpty()) null else Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-API-Key", key.apiKey)
                    .build(),
            )
        }
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .addInterceptor(levelHeader)
        apiKeyInterceptor?.let { clientBuilder.addInterceptor(it) }
        val client = clientBuilder
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .writeTimeout(connectSec, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(key.baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SaylatApi::class.java)
    }
}
