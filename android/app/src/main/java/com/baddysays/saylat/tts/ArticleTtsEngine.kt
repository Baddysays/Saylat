package com.baddysays.saylat.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class TtsStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

data class TtsState(
    val status: TtsStatus = TtsStatus.IDLE,
    val currentParagraphIndex: Int = -1,
    val totalParagraphs: Int = 0,
    val errorMessage: String? = null,
)

class ArticleTtsEngine(context: Context) {

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var paragraphs: List<String> = emptyList()
    private var currentIndex = 0
    private var initialized = false

    init {
        _state.value = TtsState(status = TtsStatus.LOADING)
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru"))
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.setLanguage(Locale.getDefault())
                }
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
                initialized = true
                _state.value = TtsState(status = TtsStatus.IDLE)
                setupProgressListener()
            } else {
                _state.value = TtsState(
                    status = TtsStatus.ERROR,
                    errorMessage = "TTS недоступен на устройстве",
                )
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.toIntOrNull() ?: return
                currentIndex = idx
                _state.value = _state.value.copy(
                    status = TtsStatus.PLAYING,
                    currentParagraphIndex = idx,
                )
            }

            override fun onDone(utteranceId: String?) {
                val idx = (utteranceId?.toIntOrNull() ?: return) + 1
                if (idx < paragraphs.size) {
                    speakParagraph(idx)
                } else {
                    _state.value = _state.value.copy(
                        status = TtsStatus.IDLE,
                        currentParagraphIndex = -1,
                    )
                }
            }

            @Deprecated("Deprecated in API")
            override fun onError(utteranceId: String?) {
                _state.value = _state.value.copy(
                    status = TtsStatus.ERROR,
                    errorMessage = "Ошибка воспроизведения",
                )
            }
        })
    }

    fun play(blocks: List<String>, startFrom: Int = 0) {
        if (!initialized) return
        paragraphs = blocks.filter { it.length > 10 }
        if (paragraphs.isEmpty()) return

        tts?.stop()
        val idx = startFrom.coerceIn(0, paragraphs.lastIndex)
        currentIndex = idx
        _state.value = TtsState(
            status = TtsStatus.PLAYING,
            currentParagraphIndex = idx,
            totalParagraphs = paragraphs.size,
        )
        speakParagraph(idx)
    }

    private fun speakParagraph(index: Int) {
        if (index >= paragraphs.size) return
        tts?.speak(
            paragraphs[index],
            TextToSpeech.QUEUE_FLUSH,
            null,
            index.toString(),
        )
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(status = TtsStatus.PAUSED)
    }

    fun resume() {
        if (_state.value.status == TtsStatus.PAUSED) {
            play(paragraphs, currentIndex)
        }
    }

    fun skipNext() {
        val next = currentIndex + 1
        if (next < paragraphs.size) {
            tts?.stop()
            speakParagraph(next)
        }
    }

    fun skipPrev() {
        val prev = (currentIndex - 1).coerceAtLeast(0)
        tts?.stop()
        speakParagraph(prev)
    }

    fun stop() {
        tts?.stop()
        paragraphs = emptyList()
        currentIndex = 0
        _state.value = TtsState(status = TtsStatus.IDLE)
    }

    fun destroy() {
        tts?.shutdown()
        tts = null
    }
}
