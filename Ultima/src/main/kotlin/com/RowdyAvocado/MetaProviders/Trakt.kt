package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.invokeExtractors
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData as UltimaLinkData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.metaproviders.TraktProvider
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink

class Trakt(val plugin: UltimaPlugin) : TraktProvider() {
    override var name = "Trakt"
    override var mainUrl = "https://trakt.tv"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Trakt)
    override val hasMainPage = true
    override val hasQuickSearch = false
    private val apiUrl = "https://api.trakt.tv"

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<UltimaLinkData>(data)
        if (mediaData.isAnime)
                invokeExtractors(Category.ANIME, mediaData, subtitleCallback, callback)
        else invokeExtractors(Category.MEDIA, mediaData, subtitleCallback, callback)
        return true
    }
}
