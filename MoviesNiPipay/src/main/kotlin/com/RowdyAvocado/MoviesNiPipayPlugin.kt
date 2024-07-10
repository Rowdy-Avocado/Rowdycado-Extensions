package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MoviesNiPipayPlugin : Plugin() {
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

    override fun load(context: Context) {
        registerMainAPI(MoviesNiPipay(this))
    }
}
