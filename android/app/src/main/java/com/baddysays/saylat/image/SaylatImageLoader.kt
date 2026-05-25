package com.baddysays.saylat.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

object SaylatImageLoader {
    fun install(context: Context) {
        val appContext = context.applicationContext
        val loader = ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("saylat_images"))
                    .maxSizePercent(0.03)
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        coil.Coil.setImageLoader(loader)
    }
}
