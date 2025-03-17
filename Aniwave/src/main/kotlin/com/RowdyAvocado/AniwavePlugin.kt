package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

enum class ServerList(val link: Pair<String, Boolean>) {
    TO("https://aniwave.to" to false),
    LI("https://aniwave.li" to false),
    VC("https://aniwave.vc" to false),
    LV("https://aniwave.lv" to true),
    BEST("https://aniwave.best" to false)
}

@CloudstreamPlugin
class AniwavePlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list
        // directly.
        registerMainAPI(Aniwave())

        this.openSettings =
                openSettings@{
                    val manager =
                            (context.getActivity() as? AppCompatActivity)?.supportFragmentManager
                                    ?: return@openSettings
                    BottomFragment(this).show(manager, "")
                }

        // if disabled server is selected by default, this will switch it to BEST server.
        ServerList.entries.find { it.link.first == currentAniwaveServer }?.let {
            if (!it.link.second) {
                currentAniwaveServer = ServerList.BEST.link.first
                reload(context)
            }
        }
    }

    fun reload(context: Context?) {
        val pluginData =
                PluginManager.getPluginsOnline().find { it.internalName.contains("Aniwave") }
        if (pluginData == null) {
            PluginManager._DO_NOT_CALL_FROM_A_PLUGIN_hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager._DO_NOT_CALL_FROM_A_PLUGIN_loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
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

        var currentAniwaveServer: String
            get() = getKey("ANIWAVE_CURRENT_SERVER") ?: ServerList.BEST.link.first
            set(value) {
                setKey("ANIWAVE_CURRENT_SERVER", value)
            }

        var aniwaveSimklSync: Boolean
            get() = getKey("ANIWAVE_SIMKL_SYNC") ?: false
            set(value) {
                setKey("ANIWAVE_SIMKL_SYNC", value)
            }
    }
}
