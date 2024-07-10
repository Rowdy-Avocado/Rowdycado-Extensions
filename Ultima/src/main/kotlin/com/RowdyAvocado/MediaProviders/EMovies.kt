package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.createSlug
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.unixTimeMS
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper

class EMoviesMediaProvider : MediaProvider() {
    override val name = "eMovies"
    override val domain = "https://emovies.si"
    override val categories = listOf(Category.MEDIA)
    private val referer = "https://embed.vodstream.xyz/"
    private val header = mapOf("X-Requested-With" to "XMLHttpRequest")

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val slug = data.title.createSlug()
        val mediaUrl =
                if (data.season == null) {
                    "$url/watch-$slug-${data.year}-1080p-hd-online-free/watching.html"
                } else {
                    val first =
                            "$url/watch-$slug-season-${data.season}-${data.year}-1080p-hd-online-free.html"
                    val second = "$url/watch-$slug-${data.year}-1080p-hd-online-free.html"
                    if (app.get(first).isSuccessful) first else second
                }

        val res = app.get(mediaUrl).document
        val id =
                (if (data.season == null) {
                            res.selectFirst("select#selectServer option[sv=oserver]")?.attr("value")
                        } else {
                            res.select("div.le-server a")
                                    .find {
                                        val num =
                                                Regex("Episode (\\d+)")
                                                        .find(it.text())
                                                        ?.groupValues
                                                        ?.get(1)
                                                        ?.toIntOrNull()
                                        num == data.episode
                                    }
                                    ?.attr("href")
                        })
                        ?.substringAfter("id=")
                        ?.substringBefore("&")

        val serverUrl = "$url/ajax/v4_get_sources?s=oserver&id=${id ?: return}&_=${unixTimeMS}"
        val server = app.get(serverUrl, headers = header).parsedSafe<EMovieServer>()?.value
        server ?: return
        val script =
                app.get(server, referer = "$url/")
                        .document
                        .selectFirst("script:containsData(sources:)")
                        ?.data()
                        ?: return
        val sources =
                Regex("sources:\\s*\\[(.*)],").find(script)?.groupValues?.get(1)?.let {
                    tryParseJson<List<EMovieSources>>("[$it]")
                }
        val tracks =
                Regex("tracks:\\s*\\[(.*)],").find(script)?.groupValues?.get(1)?.let {
                    tryParseJson<List<EMovieTraks>>("[$it]")
                }

        sources?.map { source ->
            M3u8Helper.generateM3u8(name, source.file ?: return@map, referer).forEach(callback)
        }

        tracks?.map { track ->
            subtitleCallback.invoke(
                    SubtitleFile(
                            track.label ?: "",
                            track.file ?: return@map,
                    )
            )
        }
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class EMovieServer(
            @JsonProperty("value") val value: String? = null,
    )

    data class EMovieSources(
            @JsonProperty("file") val file: String? = null,
    )

    data class EMovieTraks(
            @JsonProperty("file") val file: String? = null,
            @JsonProperty("label") val label: String? = null,
    )
    // #endregion - Data classes

}
