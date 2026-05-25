package com.baddysays.saylat.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object GallerySaver {

    suspend fun saveJpegBytes(context: Context, jpeg: ByteArray, displayName: String): String? =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/Saylat",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri.toString()
        }

    suspend fun saveDataUrl(context: Context, dataUrl: String, displayName: String): String? {
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return null
        val raw = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        return saveJpegBytes(context, raw, displayName)
    }

    suspend fun saveStripsMerged(
        context: Context,
        dataUrls: List<String>,
        baseName: String,
    ): String? = withContext(Dispatchers.IO) {
        val bitmaps = dataUrls.mapNotNull { url ->
            val comma = url.indexOf(',')
            if (comma < 0) return@mapNotNull null
            val raw = Base64.decode(url.substring(comma + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)
        }
        if (bitmaps.isEmpty()) return@withContext null
        val width = bitmaps.maxOf { it.width }
        val height = bitmaps.sumOf { it.height }
        val merged = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = android.graphics.Canvas(merged)
        var y = 0
        for (bmp in bitmaps) {
            val left = (width - bmp.width) / 2
            canvas.drawBitmap(bmp, left.toFloat(), y.toFloat(), null)
            y += bmp.height
            bmp.recycle()
        }
        val out = ByteArrayOutputStream()
        merged.compress(Bitmap.CompressFormat.JPEG, 88, out)
        merged.recycle()
        saveJpegBytes(context, out.toByteArray(), "${baseName}_полосы.jpg")
    }
}
