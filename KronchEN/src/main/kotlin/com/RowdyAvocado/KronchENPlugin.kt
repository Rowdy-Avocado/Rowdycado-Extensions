package com.RowdyAvocado

import android.os.Handler
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class KronchENPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(KronchEN())
    }

    companion object {
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post(
                    object : Runnable {
                        override fun run() {
                            function()
                        }
                    }
            )
        }
    }
}
