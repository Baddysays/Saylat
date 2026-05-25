package com.baddysays.saylat.cache

import android.content.Context
import android.util.Base64
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.StripPage
import com.baddysays.saylat.data.StripSegment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.security.MessageDigest

/** Офлайн-кэш статей и полос (JPEG на диске, как в браузере). */
object PageCache {
    private const val MAX_ARTICLES = 16
    private const val MAX_STRIP_PAGES = 12

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val articleAdapter = moshi.adapter(SaylatArticle::class.java)
    private val stripAdapter = moshi.adapter(StripPage::class.java)

    data class CachedEntry(
        val url: String,
        val title: String,
        val savedAt: Long,
        val hasArticle: Boolean,
        val hasStrips: Boolean,
        val bytesOnDisk: Long = 0,
    )

    data class CacheStats(
        val entryCount: Int = 0,
        val articleCount: Int = 0,
        val stripPageCount: Int = 0,
        val stripImageCount: Int = 0,
        val cacheBytes: Long = 0,
        val appFilesBytes: Long = 0,
    )

    fun putArticle(context: Context, article: SaylatArticle) {
        val dir = entryDir(context, article.url) ?: return
        dir.mkdirs()
        File(dir, "article.json").writeText(articleAdapter.toJson(article))
        touchMeta(dir, article.url, article.title, hasArticle = true, hasStrips = hasStripFiles(dir))
        prune(context)
    }

    fun putStripPage(context: Context, page: StripPage) {
        val dir = entryDir(context, page.url) ?: return
        dir.mkdirs()
        clearStripFiles(dir)
        val persisted = persistStripImages(dir, page)
        File(dir, "strips.json").writeText(stripAdapter.toJson(persisted))
        touchMeta(
            dir,
            page.url,
            page.title,
            hasArticle = File(dir, "article.json").isFile,
            hasStrips = true,
        )
        prune(context)
    }

    fun loadArticle(context: Context, url: String): SaylatArticle? {
        val file = File(entryDir(context, url) ?: return null, "article.json")
        if (!file.isFile) return null
        return runCatching { articleAdapter.fromJson(file.readText()) }.getOrNull()
    }

    fun loadStripPage(context: Context, url: String): StripPage? {
        val dir = entryDir(context, url) ?: return null
        val file = File(dir, "strips.json")
        if (!file.isFile) return null
        val page = runCatching { stripAdapter.fromJson(file.readText()) }.getOrNull() ?: return null
        return resolveStripSources(dir, page)
    }

    fun listRecent(context: Context): List<CachedEntry> =
        cacheRoot(context)
            .listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> readMeta(dir)?.copy(bytesOnDisk = dirSize(dir)) }
            ?.sortedByDescending { it.savedAt }
            ?: emptyList()

    fun stats(context: Context): CacheStats {
        val root = cacheRoot(context)
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        var articles = 0
        var stripPages = 0
        var stripImages = 0
        var cacheBytes = 0L
        dirs.forEach { dir ->
            cacheBytes += dirSize(dir)
            if (File(dir, "article.json").isFile) articles++
            if (File(dir, "strips.json").isFile) {
                stripPages++
                stripImages += dir.listFiles()?.count { it.name.startsWith("strip_") && it.isFile } ?: 0
            }
        }
        return CacheStats(
            entryCount = dirs.size,
            articleCount = articles,
            stripPageCount = stripPages,
            stripImageCount = stripImages,
            cacheBytes = cacheBytes,
            appFilesBytes = dirSize(context.filesDir),
        )
    }

    fun clearAll(context: Context) {
        cacheRoot(context).listFiles()?.forEach { it.deleteRecursively() }
        cacheRoot(context).mkdirs()
    }

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

    private fun persistStripImages(dir: File, page: StripPage): StripPage {
        val strips = page.strips.mapIndexed { index, seg ->
            val bytes = decodeDataUrl(seg.src)
            if (bytes != null) {
                val file = File(dir, "strip_$index.jpg")
                file.writeBytes(bytes)
                seg.copy(src = file.toURI().toString())
            } else {
                seg
            }
        }
        return page.copy(strips = strips)
    }

    private fun resolveStripSources(dir: File, page: StripPage): StripPage {
        val strips = page.strips.mapIndexed { index, seg ->
            val local = File(dir, "strip_$index.jpg")
            when {
                local.isFile -> seg.copy(src = local.toURI().toString())
                seg.src.startsWith("file:") -> seg
                else -> seg
            }
        }
        return page.copy(strips = strips)
    }

    private fun decodeDataUrl(dataUrl: String): ByteArray? {
        if (!dataUrl.startsWith("data:")) return null
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return null
        val payload = dataUrl.substring(comma + 1)
        return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
    }

    private fun clearStripFiles(dir: File) {
        dir.listFiles()?.filter { it.name.startsWith("strip_") && it.isFile }?.forEach { it.delete() }
    }

    private fun hasStripFiles(dir: File): Boolean =
        dir.listFiles()?.any { it.name.startsWith("strip_") && it.isFile } == true

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

    private fun prune(context: Context) {
        val root = cacheRoot(context)
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return
        val byTime = dirs.mapNotNull { d -> readMeta(d)?.let { d to it } }
            .sortedByDescending { it.second.savedAt }
        val stripDirs = byTime.filter { it.second.hasStrips }
        stripDirs.drop(MAX_STRIP_PAGES).forEach { (dir, _) -> dir.deleteRecursively() }
        val remaining = root.listFiles()?.filter { it.isDirectory } ?: return
        val again = remaining.mapNotNull { d -> readMeta(d)?.let { d to it } }
            .sortedByDescending { it.second.savedAt }
        again.drop(MAX_ARTICLES).forEach { (dir, _) -> dir.deleteRecursively() }
    }

    fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }
}
