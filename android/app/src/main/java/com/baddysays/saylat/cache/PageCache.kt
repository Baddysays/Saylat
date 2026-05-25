package com.baddysays.saylat.cache

import android.content.Context
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.StripPage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.security.MessageDigest

/** Последние страницы для чтения без сети. */
object PageCache {
    private const val MAX_ARTICLES = 12
    private const val MAX_STRIPS = 5
    private const val MAX_STRIPS_BYTES = 450_000

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val articleAdapter = moshi.adapter(SaylatArticle::class.java)
    private val stripAdapter = moshi.adapter(StripPage::class.java)

    data class CachedEntry(
        val url: String,
        val title: String,
        val savedAt: Long,
        val hasArticle: Boolean,
        val hasStrips: Boolean,
    )

    fun putArticle(context: Context, article: SaylatArticle) {
        val dir = entryDir(context, article.url) ?: return
        dir.mkdirs()
        File(dir, "article.json").writeText(articleAdapter.toJson(article))
        touchMeta(dir, article.url, article.title, hasArticle = true, hasStrips = File(dir, "strips.json").exists())
        prune(context, keepArticles = MAX_ARTICLES, keepStrips = MAX_STRIPS)
    }

    fun putStripPage(context: Context, page: StripPage) {
        if (page.stats.payload_bytes > MAX_STRIPS_BYTES) return
        val dir = entryDir(context, page.url) ?: return
        dir.mkdirs()
        File(dir, "strips.json").writeText(stripAdapter.toJson(page))
        val title = page.title
        touchMeta(dir, page.url, title, hasArticle = File(dir, "article.json").exists(), hasStrips = true)
        prune(context, keepArticles = MAX_ARTICLES, keepStrips = MAX_STRIPS)
    }

    fun loadArticle(context: Context, url: String): SaylatArticle? {
        val file = File(entryDir(context, url) ?: return null, "article.json")
        if (!file.isFile) return null
        return runCatching { articleAdapter.fromJson(file.readText()) }.getOrNull()
    }

    fun loadStripPage(context: Context, url: String): StripPage? {
        val file = File(entryDir(context, url) ?: return null, "strips.json")
        if (!file.isFile) return null
        return runCatching { stripAdapter.fromJson(file.readText()) }.getOrNull()
    }

    fun listRecent(context: Context): List<CachedEntry> =
        cacheRoot(context)
            .listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> readMeta(dir) }
            ?.sortedByDescending { it.savedAt }
            ?: emptyList()

    private fun cacheRoot(context: Context): File =
        File(context.filesDir, "page_cache").apply { mkdirs() }

    private fun entryDir(context: Context, url: String): File? {
        val key = urlKey(url) ?: return null
        return File(cacheRoot(context), key)
    }

    private fun urlKey(url: String): String? {
        val normalized = url.trim().lowercase()
        if (normalized.isBlank()) return null
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun touchMeta(
        dir: File,
        url: String,
        title: String,
        hasArticle: Boolean,
        hasStrips: Boolean,
    ) {
        File(dir, "meta.txt").writeText(
            listOf(
                url,
                title.replace('\n', ' ').take(200),
                System.currentTimeMillis().toString(),
                hasArticle.toString(),
                hasStrips.toString(),
            ).joinToString("\n"),
        )
    }

    private fun readMeta(dir: File): CachedEntry? {
        val lines = File(dir, "meta.txt").takeIf { it.isFile }?.readLines() ?: return null
        if (lines.size < 3) return null
        return CachedEntry(
            url = lines[0],
            title = lines.getOrElse(1) { "" },
            savedAt = lines[2].toLongOrNull() ?: 0L,
            hasArticle = lines.getOrElse(3) { "false" }.toBooleanStrictOrNull() ?: false,
            hasStrips = lines.getOrElse(4) { "false" }.toBooleanStrictOrNull() ?: false,
        )
    }

    private fun prune(context: Context, keepArticles: Int, keepStrips: Int) {
        val root = cacheRoot(context)
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return
        val byTime = dirs.mapNotNull { d -> readMeta(d)?.let { d to it } }
            .sortedByDescending { it.second.savedAt }
        val stripDirs = byTime.filter { it.second.hasStrips }
        stripDirs.drop(keepStrips).forEach { (dir, _) -> dir.deleteRecursively() }
        val remaining = root.listFiles()?.filter { it.isDirectory } ?: return
        val again = remaining.mapNotNull { d -> readMeta(d)?.let { d to it } }
            .sortedByDescending { it.second.savedAt }
        again.drop(keepArticles).forEach { (dir, _) -> dir.deleteRecursively() }
    }
}
