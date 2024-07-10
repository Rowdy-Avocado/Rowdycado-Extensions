package com.RowdyAvocado

import android.util.Base64
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor

class AllWishExtractor : ExtractorApi() {
    override val mainUrl = AllWish.mainUrl
    override val name = AllWish.name
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
                "Vidstreaming" -> {
                    val iv = "3134003223491201"
                    val secretKey = "37911490979715163134003223491201"
                    val secretDecryptKey = "54674138327930866480207815084989"
                    GogoHelper.extractVidstream(
                            url,
                            "Vidstreaming",
                            callback,
                            iv,
                            secretKey,
                            secretDecryptKey,
                            isUsingAdaptiveKeys = false,
                            isUsingAdaptiveData = true
                    )
                }
                "Filelions" -> {
                    Filelions().getUrl(url, "", subtitleCallback, callback)
                }
                "VidPlay" -> {
                    val tempRes = app.get(url, headers = AllWish.refHeader)
                    val encryptedSource = tempRes.url.substringAfterLast("?data=#")
                    if (encryptedSource.isNotBlank()) {
                        val link =
                                Base64.decode(encryptedSource, Base64.DEFAULT)
                                        .toString(Charsets.UTF_8)
                        callback.invoke(buildExtractorLink(serverName, link))
                    }
                }
                "Gogo server" -> {}
                "Streamwish" -> {
                    StreamWish().getUrl(url, "", subtitleCallback, callback)
                }
                "Mp4Upload" -> {
                    loadExtractor(url, subtitleCallback, callback)
                }
                "Doodstream" -> {
                    loadExtractor(url, subtitleCallback, callback)
                }
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

    class StreamWish : StreamWishExtractor() {
        override var name = "StreamWish"
        override var mainUrl = "https://awish.pro"
        override val requiresReferer = false
    }
}
