package com.RowdyAvocado

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor

class AnitakuExtractor : ExtractorApi() {
    override val mainUrl = AnitakuProvider.mainUrl
    override val name = AnitakuProvider.name
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val serverName = referer
        if (url.isNotEmpty()) {
            when (serverName) {
                "streamwish" -> StreamWish().getUrl(url, "", subtitleCallback, callback)
                "doodstream" -> loadExtractor(url, subtitleCallback, callback)
                "filelions" -> {
                    Filelions().getUrl(url, "", subtitleCallback, callback)
                }
                "anime" -> {
                    val link = url
                    val iv = "3134003223491201"
                    val secretKey = "37911490979715163134003223491201"
                    val secretDecryptKey = "54674138327930866480207815084989"
                    GogoHelper.extractVidstream(
                            link,
                            "Vidstreaming",
                            callback,
                            iv,
                            secretKey,
                            secretDecryptKey,
                            isUsingAdaptiveKeys = false,
                            isUsingAdaptiveData = true
                    )
                }
                "mp4upload" -> loadExtractor(url, subtitleCallback, callback)
            }
        }
    }

    private fun buildExtractorLink(
            serverName: String,
            link: String,
            referer: String = "",
            quality: Int = Qualities.Unknown.value
    ): ExtractorLink {
        return ExtractorLink(serverName, serverName, link, referer, quality, link.contains(".m3u8"))
    }

    class Filelions : VidhideExtractor() {
        override var name = "Filelions"
        override var mainUrl = "https://alions.pro"
        override val requiresReferer = false
    }

    class StreamWish : Filesim() {
        override val name = "StreamWish"
        override val mainUrl = "https://awish.pro"
        override val requiresReferer = false
    }
}
