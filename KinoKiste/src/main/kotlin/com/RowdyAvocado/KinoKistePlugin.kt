package com.RowdyAvocado

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class TemplatePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(KinoKiste())
    }
}