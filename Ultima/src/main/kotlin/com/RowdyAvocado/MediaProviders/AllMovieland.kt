package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.fixUrl
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper

class AllMovielandMediaProvider : MediaProvider() {
    override val name = "AllMovieland"
    override val domain = "https://allmovieland.fun"
    override val categories = listOf(Category.MEDIA)
    private val host = "https://zativertz295huk.com"

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val referer = "$url/"
        val res =
                app.get("$host/play/${data.imdbId}", referer = referer)
                        .document
                        .selectFirst("script:containsData(playlist)")
                        ?.data()
                        ?.substringAfter("{")
                        ?.substringBefore(";")
                        ?.substringBefore(")")
        val json = tryParseJson<AllMovielandPlaylist>("{${res ?: return}")
        val headers = mapOf("X-CSRF-TOKEN" to "${json?.key}")

        val serverRes =
                app.get(fixUrl(json?.file ?: return, host), headers = headers, referer = referer)
                        .text
                        .replace(Regex(""",\s*\[]"""), "")
        val servers =
                tryParseJson<ArrayList<AllMovielandServer>>(serverRes).let { server ->
                    if (data.season == null) {
                        server?.map { it.file to it.title }
                    } else {
                        server
                                ?.find { it.id.equals("${data.season}") }
                                ?.folder
                                ?.find { it.episode.equals("${data.episode}") }
                                ?.folder
                                ?.map { it.file to it.title }
                    }
                }

        servers?.apmap { (server, lang) ->
            val path =
                    app.post(
                                    "${host}/playlist/${server ?: return@apmap}.txt",
                                    headers = headers,
                                    referer = referer
                            )
                            .text
            M3u8Helper.generateM3u8("Allmovieland [$lang]", path, referer).forEach(callback)
        }
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class AllMovielandPlaylist(
            @JsonProperty("file") val file: String? = null,
            @JsonProperty("key") val key: String? = null,
            @JsonProperty("href") val href: String? = null,
    )

    data class AllMovielandServer(
            @JsonProperty("title") val title: String? = null,
            @JsonProperty("id") val id: String? = null,
            @JsonProperty("file") val file: String? = null,
            @JsonProperty("folder")
            val folder: ArrayList<AllMovielandSeasonFolder>? = arrayListOf(),
    ) {
        data class AllMovielandSeasonFolder(
                @JsonProperty("episode") val episode: String? = null,
                @JsonProperty("id") val id: String? = null,
                @JsonProperty("folder")
                val folder: ArrayList<AllMovielandEpisodeFolder>? = arrayListOf(),
        ) {
            data class AllMovielandEpisodeFolder(
                    @JsonProperty("title") val title: String? = null,
                    @JsonProperty("id") val id: String? = null,
                    @JsonProperty("file") val file: String? = null,
            )
        }
    }
    // #endregion - Data classes

}
