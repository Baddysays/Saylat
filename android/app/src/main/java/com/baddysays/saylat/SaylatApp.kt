package com.baddysays.saylat

import android.app.Application
import com.baddysays.saylat.image.SaylatImageLoader
import com.baddysays.saylat.prefs.SaylatPrefs

class SaylatApp : Application() {
    lateinit var prefs: SaylatPrefs
        private set

    override fun onCreate() {
        super.onCreate()
        SaylatImageLoader.install(this)
        prefs = SaylatPrefs(this)
    }
}
