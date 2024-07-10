package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class TokyBook : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://tokybook.com"
    override var name = "TokyBook Audiobook"

    override val hasMainPage = true

    // tracks = \[([\s\S]*?)buildPlaylist

    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Others)

    // Infinite loading in homepage goddamit I am not going to fix this I have no idea
    // what to do

    override val mainPage = mainPageOf("/" to "Latest")

    // taken from hexated, I have no idea wtf I am doing

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}").document
        val home = document.select("div.inside-article").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("header h2 a")?.attr("href") ?: return null)
        val title = this.selectFirst("header h2 a")?.text() ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div a img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        // return document.select("div.post-filter-image").mapNotNull {
        return document.select("div.inside-article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {

        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val poster =
                document.selectFirst("img[loading]")?.attr("src")
                        ?: "https://ww3.pelisplus.to/images/logo2.png"

        val tvType = TvType.TvSeries

        // val plot = document.selectFirst("div.description")?.text()?.trim()
        // val year = document.selectFirst("div.genres.rating span a")?.text()?.trim()?.toInt()
        // ?:null

        return if (tvType == TvType.TvSeries) {

            var jasonprepre = document.html()

            var jasonpre = Regex("""tracks = \[([\s\S]*?)]""").find(jasonprepre)?.value

            jasonpre = jasonpre?.replace("tracks = ", "")

            // val jason = parseJson<Jason>(jsonstring)

            // I HAVE TO FIX THIS JSON, WOW JUST WOW
            // val place = 380
            // jasonpre = jasonpre?.substring(0, place) + """ "url": "NA" """ +
            // jasonpre?.substring(place, jasonpre?.length!! );

            jasonpre =
                    jasonpre?.replace(
                            """{
                          "track": 1,
                          "name": "welcome",
                          "chapter_link_dropbox": "https://file.tokybook.com/upload/welcome-you-to-tokybook.mp3",
                          "duration": "8",
                          "chapter_id": "0",
                          "post_id": "0",
                          },""",
                            ""
                    )

            // val jason = parseJson<Map<String, Chapter>>(jasonpre!!)

            // val jason = parseJson<Chapter>(jasonpre!!)
            val jason = parseJson<List<Chapter>>(jasonpre!!)

            // val episodes : MutableList<Episode> = emptyList(<Episode>)
            var episodes = mutableListOf<Episode>()

            for (i in jason) { // (key,value)

                // val log = chapter
                if (i.track == 1) {
                    continue
                }
                var name = i.name // eps?.title

                val href = "https://files01.tokybook.com/audio/" + i.audiocode

                // "creativity-inc/1628010881866/001 - Creativity, Inc..mp3"
                // change to
                // https://files01.tokybook.com/audio/creativity-inc/1628010881866/001 - Creativity,
                // Inc..mp3

                episodes.add(
                        Episode(
                                href,
                                name,
                        )
                )
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster.toString()
                // this.year = year
                // this.plot = plot
                // this.recommendations = recommendations
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster.toString()
                // this.year = year
                // this.plot = plot
                // this.recommendations = recommendations
            }
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {

        val name = "TokyBook Audiobook"

        callback.invoke(
                ExtractorLink(
                        source = name,
                        name = "TokyBook Audiobook",
                        url = data,
                        referer = "$mainUrl/",
                        quality = Qualities.P360.value
                )
        )

        // callback.invoke(
        //    ExtractorLink(
        //        this.name,
        //        this.name,
        //        data.replace("\\", ""),
        //        referer = mainUrl,
        //        quality = Qualities.Unknown.value,
        //                headers = mapOf("Range" to "bytes=0-"),
        //    )
        // )
        return true
    }

    data class Chapter(
            @JsonProperty("track") val track: Int? = null, // 1,2,3,...
            @JsonProperty("name") val name: String? = null, // welcome,"001 - Creativity, Inc."
            @JsonProperty("chapter_link_dropbox") val audiocode: String? = null,
            @JsonProperty("duration") val duration: String? = null,
            @JsonProperty("chapter_id") val chapter_id: String? = null,
            @JsonProperty("post_id") val post_id: String? = null,
            @JsonProperty("url") val url: String? = null
    )
}
