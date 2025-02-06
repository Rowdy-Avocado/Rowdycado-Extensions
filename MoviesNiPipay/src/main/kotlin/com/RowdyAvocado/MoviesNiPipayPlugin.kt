package com.RowdyAvocado

import android.os.Handler
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MoviesNiPipayPlugin : BasePlugin() {
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

    override fun load() {
        registerMainAPI(MoviesNiPipay(this))
    }
}
