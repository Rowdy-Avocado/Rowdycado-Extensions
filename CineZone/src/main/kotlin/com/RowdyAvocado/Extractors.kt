package com.RowdyAvocado

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.AnyVidplay
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class CineZoneExtractor : ExtractorApi() {
    override val mainUrl = "https://cinezone.to"
    override val name = "CineZone"
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val serverName = referer
        if (url.isNotEmpty()) {
            try {
                val domain = CineZoneUtils.getBaseUrl(url)
                when (serverName) {
                    "filemoon" -> FileMoon().getUrl(url, null, subtitleCallback, callback)
                    "vidplay" -> AnyVidplay(domain).getUrl(url, null, subtitleCallback, callback)
                    "mycloud" -> AnyMyCloud(domain).getUrl(url, null, subtitleCallback, callback)
                    else -> loadExtractor(url, subtitleCallback, callback)
                }
            } catch (e: Exception) {}
        }
    }
}

class AnyMyCloud(hostUrl: String) : Vidplay() {
    override val name = "MyCloud"
    override val mainUrl = hostUrl
}
