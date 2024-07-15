package com.RowdyAvocado

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Vtbe
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class KinoKiste : MainAPI() {
    override var name = "KinoKiste"
    override var mainUrl = "https://kinokiste.live"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "de"
    override val hasMainPage = true
    override val hasDownloadSupport = true

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst("div.f_title > a")?.text() ?: return null
        val link = this.selectFirst("div.f_title > a")?.attr("href") ?: return null
        val poster =
                mainUrl.plus(this.selectFirst("div.thumb> a > img")?.attr("src") ?: return null)
        return newMovieSearchResponse(title, link) { this.posterUrl = poster }
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/filme/" to "Filme",
                    "$mainUrl/kinofilme/" to "Filme im Kino",
                    "$mainUrl/serien/" to "Serien"
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res = app.get(request.data.plus(if (page.equals(1)) "" else "page/$page"))
        if (!res.code.equals(200)) return null
        val list =
                res.document.select("div#dle-content > div.item").mapNotNull {
                    it.toSearchResponse()
                }
        return newHomePageResponse(request, list, true)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val res = app.get("$mainUrl/?do=search&subaction=search&story=$query")
        if (!res.code.equals(200)) return null
        val list =
                res.document.select("div#dle-content > div.item").mapNotNull {
                    it.toSearchResponse()
                }
        return list
    }

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url)
        if (!res.code.equals(200)) throw ErrorLoadingException("Unable to fetch page")
        val title =
                res.document.selectFirst("#title span")?.text()
                        ?: throw ErrorLoadingException("Unable to read title")
        val desc = res.document.selectFirst("#storyline p")?.text()
        val poster = mainUrl.plus(res.document.selectFirst("img#poster_path_large")?.attr("src"))
        val bgStyle = res.document.selectFirst("#dle-content > style[media=screen]")?.html() ?: ""
        val bgPoster =
                mainUrl.plus(Regex("url\\(\"(.*)\"\\)").find(bgStyle)?.destructured?.component1())
        val genres =
                res.document.select("#longInfo div div:nth-child(3) div a").mapNotNull { it.text() }
        val seriesData = res.document.selectFirst("div.serie-menu")
        val imdbLink = res.document.selectFirst("a[href*=imdb]")?.attr("href") ?: ""
        val imdbId = Regex("/(tt.*)/").find(imdbLink)?.destructured?.component1()

        if (seriesData == null) {
            val iFrameUrl = res.document.selectFirst("#info iframe[width]")?.attr("src")
            return newMovieLoadResponse(title, url, TvType.Movie, iFrameUrl) {
                this.plot = desc
                this.posterUrl = poster
                this.backgroundPosterUrl = bgPoster
                this.tags = genres
                addImdbId(imdbId)
            }
        }
        val episodes =
                seriesData.select("div.tt_series ul > li").map {
                    val (season, episode) = it.select("a").attr("data-num").split("x")
                    newEpisode(it.selectFirst("div.mirrors")?.html()) {
                        this.name = it.selectFirst("a")?.attr("data-title")
                        this.season = season.toInt()
                        this.episode = episode.toInt()
                    }
                }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.plot = desc
            this.posterUrl = poster
            this.backgroundPosterUrl = bgPoster
            this.tags = genres
            addImdbId(imdbId)
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        var linkPairs = listOf<Pair<String, String>>()
        if (data.contains(mainUrl)) {
            var mirrorsData = Jsoup.parse(data.removeSuffix("$mainUrl/"))
            mirrorsData.select("a").forEach {
                linkPairs += it.attr("data-m") to it.attr("data-link")
            }
        } else {
            val res = app.get(data).document
            res.select("li[data-link]").forEach {
                linkPairs += it.text() to "https:".plus(it.attr("data-link"))
            }
        }
        linkPairs.amap {
            when (it.first) {
                "supervideo" -> {}
                "dropload" -> Dropload().getUrl(it.second, it.second)?.amap { callback.invoke(it) }
                "mixdrop" -> loadExtractor(it.second, subtitleCallback, callback)
                "doodstream" ->
                        AnyDoodStreamExtractor(getBaseUrl(it.second))
                                .getUrl(it.second, it.second)
                                ?.amap { callback.invoke(it) }
                else -> {}
            }
        }
        return true
    }

    // util methods
    fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }

    class Dropload : Vtbe() {
        override var name = "Dropload"
        override var mainUrl = "https://dropload.io"
        override val requiresReferer = true
    }

    // extractors
    open class AnyDoodStreamExtractor(domain: String) : ExtractorApi() {
        override var name = "DoodStream"
        override var mainUrl = domain
        override val requiresReferer = false

        override fun getExtractorUrl(id: String): String {
            return "$mainUrl/d/$id"
        }

        override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
            val res = app.get(url).text
            val md5 = mainUrl + (Regex("/pass_md5/[^']*").find(res)?.value ?: return null)
            val res2 = app.get(md5, referer = url).text
            val trueUrl = res2 + "zUEJeL3mUN?token=" + md5.substringAfterLast("/")
            val quality =
                    Regex("\\d{3,4}p")
                            .find(res.substringAfter("<title>").substringBefore("</title>"))
                            ?.groupValues
                            ?.get(0)
            return listOf(
                    ExtractorLink(
                            this.name,
                            this.name,
                            trueUrl,
                            mainUrl,
                            getQualityFromName(quality),
                            false
                    )
            )
        }
    }
}
