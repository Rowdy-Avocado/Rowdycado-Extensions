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
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class RidomoviesMediaProvider : MediaProvider() {
    override val name = "Ridomovies"
    override val domain = "https://ridomovies.tv"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val id = data.imdbId ?: data.tmdbId ?: return
        val mediaSlug =
                app.get("$url/core/api/search?q=$id")
                        .parsedSafe<RidoSearch>()
                        ?.data
                        ?.items
                        ?.find {
                            it.contentable?.tmdbId == data.tmdbId ||
                                    it.contentable?.imdbId == data.imdbId
                        }
                        ?.slug
                        ?: return

        val mediaId =
                data.season?.let {
                    val episodeUrl = "$url/tv/$mediaSlug/season-$it/episode-${data.episode}"
                    app.get(episodeUrl).text.substringAfterLast("postid").substringBefore("\\")
                }
                        ?: mediaSlug

        val mediaUrl =
                "$url/core/api/${if (data.season == null) "movies" else "episodes"}/$mediaId/videos"
        app.get(mediaUrl).parsedSafe<RidoResponses>()?.data?.apmap { link ->
            val iframe = Jsoup.parse(link.url ?: return@apmap).select("iframe").attr("data-src")
            if (iframe.startsWith("https://closeload.top")) {
                val unpacked = getAndUnpack(app.get(iframe, referer = "$url/").text)
                val video = Regex("=\"(aHR.*?)\";").find(unpacked)?.groupValues?.get(1)
                commonLinkLoader(
                        name,
                        ServerName.Custom,
                        base64Decode(video ?: return@apmap),
                        getBaseUrl(iframe),
                        null,
                        subtitleCallback,
                        callback,
                        Qualities.P1080.value,
                        true
                )
            } else {
                loadExtractor(iframe, "$url/", subtitleCallback, callback)
            }
        }
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class RidoData(
            @JsonProperty("url") var url: String? = null,
            @JsonProperty("items") var items: ArrayList<RidoItems>? = arrayListOf(),
    ) {
        data class RidoItems(
                @JsonProperty("slug") var slug: String? = null,
                @JsonProperty("contentable") var contentable: RidoContentable? = null,
        ) {
            data class RidoContentable(
                    @JsonProperty("imdbId") var imdbId: String? = null,
                    @JsonProperty("tmdbId") var tmdbId: Int? = null,
            )
        }
    }

    data class RidoSearch(
            @JsonProperty("data") var data: RidoData? = null,
    )

    data class RidoResponses(
            @JsonProperty("data") var data: ArrayList<RidoData>? = arrayListOf(),
    )
    // #endregion - Data classes
}
