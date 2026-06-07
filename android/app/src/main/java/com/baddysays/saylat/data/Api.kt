package com.baddysays.saylat.data



import com.baddysays.saylat.network.SaylatHttpClient

import com.squareup.moshi.Moshi

import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import okhttp3.Interceptor

import okhttp3.OkHttpClient

import retrofit2.Retrofit

import retrofit2.converter.moshi.MoshiConverterFactory

import okhttp3.ResponseBody

import retrofit2.Response

import retrofit2.http.Body

import retrofit2.http.GET

import retrofit2.http.POST

import retrofit2.http.Query



interface SaylatApi {

    @GET("api/extract")

    suspend fun extract(

        @Query("url") url: String,

        @Query("images") images: String = "normal",

        @Query("level") level: String = "medium",

    ): ArticleWireEnvelope



    @GET("api/extract/binary")

    suspend fun extractBinary(

        @Query("url") url: String,

        @Query("images") images: String = "normal",

        @Query("level") level: String = "medium",

    ): Response<ResponseBody>



    @POST("api/open/binary")

    suspend fun openBinary(@Body body: OpenRequest): Response<ResponseBody>



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



    @POST("api/cache/invalidate")

    suspend fun invalidateCache(@Query("url") url: String): Map<String, @JvmSuppressWildcards Any>



    @GET("api/feed")

    suspend fun unifiedFeed(

        @Query("limit") limit: Int = 12,

        @Query("offset") offset: Int = 0,

        @Query("page_size") pageSize: Int = 24,

    ): SaylatFeed



    @GET("api/rss/feed")

    suspend fun rssFeed(@Query("url") url: String): SaylatFeed



    @GET("api/rss/discover")

    suspend fun rssDiscover(@Query("url") url: String): Map<String, String>

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



    @Volatile

    private var cachedClient: OkHttpClient? = null



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

            SaylatHttpClient.evictConnections(cachedClient)

            cachedClient = null

            cached = null

        }

    }



    fun invalidateDns(hostname: String) {

        SaylatHttpClient.invalidateDns(hostname)

    }



    private fun buildClient(key: ClientKey): SaylatApi {

        val levelHeader = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("X-Saylat-Level", key.compressionLevel)
                .header(PayloadCodec.HEADER_CODEC, PayloadCodec.HEADER_CODEC_VALUE)
            if (key.slowNetwork) {
                builder.header("X-Saylat-Slow-Network", "1")
            }
            chain.proceed(builder.build())
        }

        val apiKeyInterceptor = if (key.apiKey.isEmpty()) null else Interceptor { chain ->

            chain.proceed(

                chain.request().newBuilder()

                    .header("X-API-Key", key.apiKey)

                    .build(),

            )

        }

        val interceptors = buildList {

            add(levelHeader)

            apiKeyInterceptor?.let { add(it) }

        }

        val client = SaylatHttpClient.build(

            slowNetwork = key.slowNetwork,

            extraInterceptors = interceptors,

        )

        cachedClient = client

        return Retrofit.Builder()

            .baseUrl(key.baseUrl)

            .client(client)

            .addConverterFactory(MoshiConverterFactory.create(moshi))

            .build()

            .create(SaylatApi::class.java)

    }

}


