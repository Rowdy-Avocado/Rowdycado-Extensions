package com.RowdyAvocado

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class MangaDexPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    var dataSaver: Boolean
        get() = getKey("MANGADEX_DATA_SAVER") ?: false
        set(value) {
            setKey("MANGADEX_DATA_SAVER", value)
        }

    override fun load(context: Context) {
        activity = context as AppCompatActivity

        // All providers should be added in this manner
        registerMainAPI(MangaDex(this))

        openSettings = {
            val frag = MangaDexSettings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
    }

    fun reload(context: Context?) {
        val pluginData =
                PluginManager.getPluginsOnline().find { it.internalName.contains("MangaDex") }
        if (pluginData == null) {
            PluginManager._DO_NOT_CALL_FROM_A_PLUGIN_hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager._DO_NOT_CALL_FROM_A_PLUGIN_loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
    }

    fun loadChapterProviders(chapterGroup: MutableList<ChapterData>) {
        val frag = MangaDexChapterProvidersFragment(this, chapterGroup)
        frag.show(activity!!.supportFragmentManager, "")
    }

    fun loadChapter(chapterName: String, pages: List<String>) {
        val frag = MangaDexChapterFragment(this, chapterName, pages)
        frag.show(activity!!.supportFragmentManager, "")
    }
}
