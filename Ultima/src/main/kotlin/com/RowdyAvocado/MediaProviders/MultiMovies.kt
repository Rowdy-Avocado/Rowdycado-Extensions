package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName
import com.RowdyAvocado.UltimaMediaProvidersUtils.commonLinkLoader
import com.RowdyAvocado.UltimaMediaProvidersUtils.createSlug
import com.RowdyAvocado.UltimaMediaProvidersUtils.getBaseUrl
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink

class MultiMoviesProvider : MediaProvider() {
    override val name = "MultiMovies"
    override val domain = "https://multimovies.icu"
    override val categories = listOf(Category.MEDIA)
    private val xmlHeader = mapOf("X-Requested-With" to "XMLHttpRequest")

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = data.title.createSlug()
        val mediaurl =
                if (data.season == null) {
                    "$url/movies/$fixTitle"
                } else {
                    "$url/episodes/$fixTitle-${data.season}x${data.episode}"
                }
        val req = app.get(mediaurl).document
        req.select("ul#playeroptionsul li").apmap {
            val id = it.attr("data-post")
            val nume = it.attr("data-nume")
            val type = it.attr("data-type")
            if (nume.contains("trailer")) return@apmap
            val apiUrl = "$url/wp-admin/admin-ajax.php"
            val postData =
                    mapOf(
                            "action" to "doo_player_ajax",
                            "post" to id,
                            "nume" to nume,
                            "type" to type
                    )
            val source =
                    app.post(url = apiUrl, data = postData, referer = url, headers = xmlHeader)
                            .parsed<ResponseHash>()
                            .embed_url
            val link = source.substringAfter("\"").substringBefore("\"")
            val domain = getBaseUrl(link)
            val serverName =
                    when (domain) {
                        "https://aa.clonimeziud" -> ServerName.Vidhide
                        "https://server2.shop" -> ServerName.Vidhide
                        "https://multimovies.cloud" -> ServerName.StreamWish
                        "https://allinonedownloader.fun" -> ServerName.StreamWish
                        else -> ServerName.NONE
                    }
            commonLinkLoader(name, serverName, link, null, null, subtitleCallback, callback)
            // when (domain) {
            //     "https://server2.shop" ->
            //             commonLinkLoader(
            //                     name,
            //                     Vidhide,
            //                     link,
            //                     null,
            //                     null,
            //                     subtitleCallback,
            //                     callback
            //             )
            //     "https://multimovies.cloud" ->
            //             commonLinkLoader(
            //                     name,
            //                     StreamWish,
            //                     link,
            //                     null,
            //                     null,
            //                     subtitleCallback,
            //                     callback
            //             )
            //     "https://allinonedownloader.fun" ->
            //             commonLinkLoader(
            //                     name,
            //                     StreamWish,
            //                     link,
            //                     null,
            //                     null,
            //                     subtitleCallback,
            //                     callback
            //             )
            //     "https://aa.clonimeziud" ->
            //             commonLinkLoader(
            //                     name,
            //                     Vidhide,
            //                     link,
            //                     null,
            //                     null,
            //                     subtitleCallback,
            //                     callback
            //             )
            // }
        }
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes

    data class ResponseHash(
            @JsonProperty("embed_url") val embed_url: String,
            @JsonProperty("key") val key: String? = null,
            @JsonProperty("type") val type: String? = null,
    )
    // #endregion - Data classes
}
