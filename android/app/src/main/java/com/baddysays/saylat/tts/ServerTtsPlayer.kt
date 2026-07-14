package com.baddysays.saylat.tts

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Озвучка статьи через /api/tts (edge-tts на сервере) — для 2G. */
class ServerTtsPlayer(private val context: Context) {
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var tempFile: File? = null

    suspend fun play(
        client: OkHttpClient,
        baseUrl: String,
        articleUrl: String,
        apiKey: String,
    ) = withContext(Dispatchers.IO) {
        stop()
        _state.value = TtsState(status = TtsStatus.LOADING)
        val root = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val httpUrl = (root + "api/tts").toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("url", articleUrl)
            .addQueryParameter("voice", "ru-m")
            .build()
        val builder = Request.Builder().url(httpUrl).header("X-Saylat-Slow-Network", "1")
        if (apiKey.isNotBlank()) builder.header("X-API-Key", apiKey)

        val bytes = client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) error("TTS HTTP ${response.code}")
            response.body?.bytes() ?: error("Empty TTS body")
        }
        if (bytes.size > 15_000_000) error("TTS payload too large (${bytes.size} bytes)")

        val file = File.createTempFile("saylat-tts-", ".mp3", context.cacheDir)
        file.writeBytes(bytes)
        tempFile = file

        withContext(Dispatchers.Main) {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    _state.value = TtsState(status = TtsStatus.PLAYING, currentParagraphIndex = 0, totalParagraphs = 1)
                    start()
                }
                setOnCompletionListener {
                    _state.value = TtsState(status = TtsStatus.IDLE)
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = TtsState(status = TtsStatus.ERROR, errorMessage = "Ошибка воспроизведения TTS")
                    true
                }
                prepareAsync()
            }
        }
    }

    fun pause() {
        player?.pause()
        _state.value = _state.value.copy(status = TtsStatus.PAUSED)
    }

    fun resume() {
        player?.start()
        _state.value = _state.value.copy(status = TtsStatus.PLAYING)
    }

    fun stop() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
        tempFile?.delete()
        tempFile = null
        _state.value = TtsState()
    }

    fun destroy() = stop()
}
