package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.getQualityFromString
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CineZone(val plugin: CineZonePlugin) : MainAPI() {
    override var mainUrl = "https://cinezone.to"
    override var name = "CineZone"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    companion object {
        var keys: Pair<String, String>? = null
    }

    private suspend fun getKeys(): Pair<String, String> {
        if (keys == null) {
            val res =
                    app.get("https://rowdy-avocado.github.io/multi-keys/").parsedSafe<Keys>()
                            ?: throw Exception("Unable to fetch keys")
            keys = res.keys.first() to res.keys.last()
        }
        return keys!!
    }

    private fun searchResponseBuilder(webDocument: Document): List<SearchResponse> {
        val searchCollection =
                webDocument.select("div.item").mapNotNull { element ->
                    val title = element.selectFirst("a.title")?.text() ?: ""
                    val link = mainUrl + element.selectFirst("a.title")?.attr("href")
                    newMovieSearchResponse(title, link) {
                        this.posterUrl = element.selectFirst("img")?.attr("data-src")
                        this.quality = getQualityFromString(element.selectFirst("b")?.text())
                    }
                }
        return searchCollection
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/filter?keyword=$query"
        val res = app.get(url)
        return searchResponseBuilder(res.document)
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/movie?page=" to "Recently updated movies",
                    "$mainUrl/tv?page=" to "Recently updated TV shows",
                    "$mainUrl/filter?type[]=movie&sort=trending&page=" to "Trending movies",
                    "$mainUrl/filter?type[]=tv&sort=trending&page=" to "Trending TV shows"
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val res = app.get(url)
        if (res.code != 200 || res.document.title().contains("WAF"))
                throw ErrorLoadingException("Unable to connect to the domain $mainUrl")
        val home = searchResponseBuilder(res.document)
        return newHomePageResponse(HomePageList(request.name, home), true)
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url)

        if (res.code != 200 || res.document.title().contains("WAF"))
                throw ErrorLoadingException("Could not load data ")
        res.document.selectFirst("section#playerDetail")?.let { details ->
            val contentId = res.document.select("body main > div.container").attr("data-id")
            val name = details.selectFirst("div.body > h1")?.ownText() ?: ""
            val releaseDate = details.select("div.meta div > div:contains(Release:) + span").text()
            val bgPosterData = res.document.selectFirst("div.playerBG")?.attr("style")
            val bgPoster = Regex("'(http.*)'").find(bgPosterData ?: "")?.destructured?.component1()

            if (url.contains("movie")) {
                val id =
                        apiCall("episode/list", contentId)
                                ?.selectFirst("ul.episodes > li > a")
                                ?.attr("data-id")
                return newMovieLoadResponse(name, url, TvType.Movie, id) {
                    this.plot = details.select("div.description").text()
                    this.year = releaseDate.split(",").get(1).trim().toIntOrNull()
                    this.posterUrl = res.document.select("div.poster > div > img").attr("src")
                    this.backgroundPosterUrl = bgPoster ?: posterUrl
                    this.rating =
                            details.selectFirst("span.imdb")?.text()?.trim()?.toFloat()?.toInt()
                    this.recommendations = searchResponseBuilder(res.document)
                }
            } else {
                val episodes = mutableListOf<Episode>()
                apiCall("episode/list", contentId)?.select("ul.episodes")?.forEach { season ->
                    season.select("li > a").forEach { episode ->
                        val epId = episode.attr("data-id")
                        episodes.add(
                                newEpisode(epId) {
                                    this.name = episode.select("span").text()
                                    this.season = season.attr("data-season").trim().toInt()
                                    this.episode = episode.attr("data-num").trim().toInt()
                                    this.data = epId
                                }
                        )
                    }
                }
                return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
                    this.plot = details.select("div.description").text()
                    this.year = releaseDate.split(",").get(1).trim().toInt()
                    this.posterUrl = res.document.select("div.poster > div > img").attr("src")
                    this.backgroundPosterUrl = bgPoster ?: posterUrl
                    this.rating =
                            details.selectFirst("span.imdb")?.text()?.trim()?.toFloat()?.toInt()
                    this.recommendations = searchResponseBuilder(res.document)
                }
            }
        }
        throw ErrorLoadingException("Could not load data" + url)
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val episodeId = data.replace(mainUrl + "/", "")
        val subtitles = getSubtitles(episodeId)
        subtitles.forEach { subtitleCallback.invoke(SubtitleFile(it.lang, it.file)) }

        val serversRes = apiCall("server/list", episodeId)
        serversRes?.select("span.server")?.forEach { server ->
            val sName = serverName(server.attr("data-id"))
            val sId = server.attr("data-link-id")
            val url = getServerUrl(sId)
            CineZoneExtractor().getUrl(url, sName, subtitleCallback, callback)
        }
        return true
    }

    private suspend fun apiCall(prefix: String, data: String): Document? {
        val vrf = CineZoneUtils.vrfEncrypt(getKeys().first, data)
        val res = app.get("$mainUrl/ajax/$prefix/$data?vrf=$vrf").parsedSafe<APIResponseHTML>()
        if (res?.status == 200) {
            return res.html
        }
        return null
    }

    private suspend fun getServerUrl(data: String): String {
        val vrf = CineZoneUtils.vrfEncrypt(getKeys().first, data)
        val res = app.get("$mainUrl/ajax/server/$data?vrf=$vrf").parsedSafe<APIResponseJSON>()
        if (res?.status == 200) {
            return CineZoneUtils.vrfDecrypt(getKeys().second, res.result.url)
        }
        return ""
    }

    private suspend fun getSubtitles(episodeId: String): Array<CineZoneSubtitle> {
        val res =
                app.get("$mainUrl/ajax/episode/subtitles/$episodeId")
                        .parsedSafe<Array<CineZoneSubtitle>>()
        return res ?: throw Exception("Unable to fetch Subtitles")
    }

    private fun serverName(serverID: String?): String? {
        val sss =
                when (serverID) {
                    "41" -> "vidplay"
                    "45" -> "filemoon"
                    "40" -> "streamtape"
                    "35" -> "mp4upload"
                    "28" -> "mycloud"
                    else -> null
                }
        return sss
    }

    data class APIResponseHTML(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: String,
            val html: Document = Jsoup.parse(result)
    )

    data class APIResponseJSON(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: ServerUrl,
    )

    data class ServerUrl(
            @JsonProperty("url") val url: String,
    )

    data class CineZoneSubtitle(
            @JsonProperty("file") val file: String,
            @JsonProperty("label") val lang: String,
            @JsonProperty("kind") val kind: String,
    )

    data class Keys(@JsonProperty("cinezone") val keys: List<String>)
}
