package com.baddysays.saylat.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SSE /api/extract/progressive — ранний показ meta и blocks на 2G.
 */
object ProgressiveArticleLoader {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val blockListType = Types.newParameterizedType(List::class.java, Block::class.java)
    private val blockListAdapter: JsonAdapter<List<Block>> = moshi.adapter(blockListType)
    private val linkListAdapter: JsonAdapter<List<ArticleLink>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, ArticleLink::class.java))
    private val cssAdapter = moshi.adapter(CssHints::class.java)

    suspend fun load(
        client: OkHttpClient,
        baseUrl: String,
        url: String,
        images: String,
        level: String,
        apiKey: String,
        onPartial: suspend (SaylatArticle) -> Unit,
    ): SaylatArticle = withContext(Dispatchers.IO) {
        val root = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val httpUrl = (root + "api/extract/progressive").toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("url", url)
            .addQueryParameter("images", images)
            .addQueryParameter("level", level)
            .build()

        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .header("Accept", "text/event-stream")
            .header(PayloadCodec.HEADER_CODEC, PayloadCodec.HEADER_CODEC_VALUE)
            .header("X-Saylat-Slow-Network", "1")
        if (apiKey.isNotBlank()) requestBuilder.header("X-API-Key", apiKey)

        client.newCall(requestBuilder.build()).execute().use { response ->
            require(response.isSuccessful) { "Progressive HTTP ${response.code}" }
            val body = response.body ?: error("Empty SSE body")
            var article = SaylatArticle(url = url, title = "")
            val blocks = mutableListOf<Block>()
            var eventName = ""
            val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
            while (true) {
                val line = reader.readLine() ?: break
                when {
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> {
                        val json = line.removePrefix("data:").trim()
                        if (json.isEmpty()) continue
                        article = mergeEvent(article, blocks, eventName, json)
                        if (eventName in setOf("meta", "blocks", "images")) {
                            onPartial(article.copy(blocks = blocks.toList()))
                        }
                    }
                    line.isBlank() -> eventName = ""
                }
            }
            article.copy(blocks = blocks.toList())
        }
    }

    private fun mergeEvent(
        article: SaylatArticle,
        blocks: MutableList<Block>,
        event: String,
        json: String,
    ): SaylatArticle {
        val obj = JSONObject(json)
        return when (event) {
            "meta" -> article.copy(
                url = obj.optString("url", article.url),
                title = obj.optString("title", article.title),
                excerpt = obj.optString("excerpt", article.excerpt),
                byline = obj.optString("byline", article.byline),
                lang = obj.optString("lang", article.lang),
                layout_hint = obj.optString("layout_hint", article.layout_hint),
                site_profile = obj.optString("site_profile", article.site_profile),
            )
            "blocks", "images" -> {
                val parsed = blockListAdapter.fromJson(obj.getJSONArray("blocks").toString()) ?: emptyList()
                if (event == "blocks" && obj.optInt("priority", 1) == 1) {
                    blocks.clear()
                    blocks.addAll(parsed)
                } else {
                    blocks.addAll(parsed)
                }
                article
            }
            "links" -> {
                var next = article
                if (obj.has("links")) {
                    val links = linkListAdapter.fromJson(obj.getJSONArray("links").toString()) ?: emptyList()
                    next = next.copy(links = links)
                }
                if (obj.has("css_hints")) {
                    val hints = cssAdapter.fromJson(obj.getJSONObject("css_hints").toString())
                    next = next.copy(css_hints = hints)
                }
                next
            }
            "stats" -> article.copy(
                stats = ArticleStats(
                    original_bytes = obj.optInt("original_bytes"),
                    payload_bytes = obj.optInt("payload_bytes"),
                    fetch_ms = obj.optInt("fetch_ms"),
                    images_inlined = obj.optInt("images_inlined"),
                    images_omitted = obj.optInt("images_omitted"),
                ),
            )
            else -> article
        }
    }
}
