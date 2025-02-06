package com.RowdyAvocado

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class TokyBookPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(TokyBook())
    }
}
