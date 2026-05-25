package com.baddysays.saylat.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
}

object ApiFactory {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun create(baseUrl: String, slowNetwork: Boolean = false): SaylatApi {
        val connectSec = if (slowNetwork) 60L else 30L
        val readSec = if (slowNetwork) 180L else 90L
        val client = OkHttpClient.Builder()
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .writeTimeout(connectSec, TimeUnit.SECONDS)
            .build()
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SaylatApi::class.java)
    }
}
