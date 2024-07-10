package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName
import com.RowdyAvocado.UltimaMediaProvidersUtils.commonLinkLoader
import com.RowdyAvocado.UltimaMediaProvidersUtils.getBaseUrl
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack

class MoflixMediaProvider : MediaProvider() {
    override val name = "Moflix"
    override val domain = "https://moflix-stream.xyz"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        // TO-DO: Fix Mirrors (for reference Avengers: Infinity War: Trakt)
        data.tmdbId ?: return
        val id =
                (if (data.season == null) {
                            "tmdb|movie|${data.tmdbId}"
                        } else {
                            "tmdb|series|${data.tmdbId}"
                        })
                        .let { base64Encode(it.toByteArray()) }

        val loaderUrl = "$url/api/v1/titles/$id?loader=titlePage"
        val url2 =
                if (data.season == null) {
                    loaderUrl
                } else {
                    val mediaId =
                            app.get(loaderUrl, referer = "$url/")
                                    .parsedSafe<MoflixResponse>()
                                    ?.title
                                    ?.id
                    "$url/api/v1/titles/$mediaId/seasons/${data.season}/episodes/${data.episode}?loader=episodePage"
                }

        val res = app.get(url2, referer = "$url/").parsedSafe<MoflixResponse>()
        (res?.episode ?: res?.title)?.videos?.filter { it.category.equals("full", true) }?.apmap {
                iframe ->
            val response = app.get(iframe.src ?: return@apmap, referer = "$url/")
            val host = getBaseUrl(iframe.src)
            val doc = response.document.selectFirst("script:containsData(sources:)")?.data()
            val script =
                    if (doc.isNullOrEmpty()) {
                        getAndUnpack(response.text)
                    } else {
                        doc
                    }
            val m3u8 = Regex("file:\\s*\"(.*?m3u8.*?)\"").find(script)?.groupValues?.getOrNull(1)
            // not sure why this line messes with loading
            // if (CommonUtils.haveDub(m3u8 ?: return@apmap, "$host/") == false) return@apmap
            m3u8 ?: return@apmap
            val quality = iframe.quality?.filter { it.isDigit() }?.toIntOrNull()
            commonLinkLoader(
                    "$name:${iframe.name}",
                    ServerName.Custom,
                    m3u8,
                    "$host/",
                    null,
                    subtitleCallback,
                    callback,
                    quality ?: Qualities.Unknown.value,
                    true
            )
        }
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class MoflixResponse(
            @JsonProperty("title") val title: Episode? = null,
            @JsonProperty("episode") val episode: Episode? = null,
    ) {
        data class Episode(
                @JsonProperty("id") val id: Int? = null,
                @JsonProperty("videos") val videos: ArrayList<Videos>? = arrayListOf(),
        ) {
            data class Videos(
                    @JsonProperty("name") val name: String? = null,
                    @JsonProperty("category") val category: String? = null,
                    @JsonProperty("src") val src: String? = null,
                    @JsonProperty("quality") val quality: String? = null,
            )
        }
    }
    // #endregion - Data classes
}
