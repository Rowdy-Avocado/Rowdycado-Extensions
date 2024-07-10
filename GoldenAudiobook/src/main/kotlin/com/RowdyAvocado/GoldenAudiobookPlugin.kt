package com.RowdyAvocado

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GoldenAudiobookPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(GoldenAudiobook())
    }
}
