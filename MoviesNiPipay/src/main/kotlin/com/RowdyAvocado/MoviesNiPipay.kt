package com.RowdyAvocado

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.VidSrcTo
import com.lagradost.cloudstream3.getQualityFromString
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlin.collections.first
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MoviesNiPipay(val plugin: MoviesNiPipayPlugin) : MainAPI() {
    override var mainUrl = "https://moviesnipipay.me"
    override var name = "MoviesNiPipay"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "fil"
    override val hasMainPage = true

    val mapper = jacksonObjectMapper()

    override val mainPage =
            mainPageOf(
                    "$mainUrl/latest-movies/page/###" to "Latest movies",
                    "$mainUrl/advanced-search/page/###/?order=popular&type[]=post" to
                            "Most Popular movies",
                    "$mainUrl/years/2024/page/###" to "2024 movies",
                    "$mainUrl/category/re-uploaded/page/###" to "Recently updated movies",
                    "$mainUrl/country/philippines/page/###" to "Philippines movies",
                    "$mainUrl/series/page/###" to "Latest Series",
                    "$mainUrl/advanced-search/page/###/?order=popular&type[]=series" to
                            "Most Popular series"
            )

    private fun searchResponseBuilder(res: Document): List<SearchResponse> {
        return res.select("article.box").mapNotNull {
            val name = it.selectFirst("h2.entry-title")?.text() ?: ""
            val url = it.selectFirst("a.tip")?.attr("href") ?: ""
            val quality =
                    it.selectFirst("span.quality")?.text()?.split(" ")?.first()?.replace("-", "")
            newMovieSearchResponse(name, url) {
                this.posterUrl = it.selectFirst("img")?.attr("src")
                this.quality = getQualityFromString(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.get("$mainUrl/?s=$query").document
        return searchResponseBuilder(res)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res = app.get(request.data.replace("###", page.toString()))
        if (res.code != 200) return null
        val searchRes = searchResponseBuilder(res.document)
        return newHomePageResponse(request.name, searchRes, true)
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title =
                doc.selectFirst("div.infodb h1.entry-title")?.text()
                        ?: throw NotImplementedError("Failed to load data")
        val poster = doc.selectFirst("div.infodb img[itemprop=image]")?.attr("src")
        val desc = doc.selectFirst("div.infodb div[itemprop=description] > p")?.text()
        val year =
                doc.selectFirst("div.infodb time[itemprop=dateCreated]")
                        ?.text()
                        ?.split("-")
                        ?.first()
                        ?.toIntOrNull()
        val tags = doc.select("div.infodb span[itemprop=genre] a").map { it.text() }
        val duration = doc.select("div.infodb span[property=duration]").text()

        if (doc.selectFirst("div.breadcrumb > span:nth-child(3) > a > span")
                        ?.text()
                        .equals("Movies")
        ) {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = desc
                this.year = year
                this.tags = tags
                this.recommendations = searchResponseBuilder(doc)
                addDuration(duration)
            }
        }
        val sEpRegex2 = Regex("""S\s*(\d)\s*EP\s*(\d)""")
        val episodes =
                doc.select("div.ts-ep-list a").map { epLink ->
                    val link = epLink.attr("href")
                    val data = epLink.selectFirst("div.epl-num")?.text() ?: ""
                    val (sNum, epNum) =
                            sEpRegex2.find(data)?.destructured ?: throw NotImplementedError()
                    newEpisode(link) {
                        this.name = epLink.selectFirst("div.epl-title")?.text()
                        this.episode = epNum.toIntOrNull()
                        this.season = sNum.toIntOrNull()
                    }
                }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = desc
            this.year = year
            this.tags = tags
            this.recommendations = searchResponseBuilder(doc)
            addDuration(duration)
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("div.server a").amap { server ->
            val encodedData = server.attr("data-em")
            val decodedData = base64Decode(encodedData)
            val iFrame = Jsoup.parse(decodedData).select("iframe")
            val link = iFrame.attr("src").trim()
            when (server.text().trim()) {
                "Vidhide" -> loadExtractor(link, mainUrl, subtitleCallback, callback)
                "Vidstream" ->
                        VidSrcTo().getUrl(link, "https://vidsrc.to", subtitleCallback, callback)
                "Embedrise" -> {
                    val res = app.get(link).document
                    res.select("video#player source").amap {
                        val finalLink = it.attr("src")
                        callback.invoke(
                                ExtractorLink(
                                        "Embedrise",
                                        "Embedrise",
                                        finalLink,
                                        "$mainUrl/",
                                        Qualities.Unknown.value,
                                        isM3u8 = finalLink.contains(".m3u8")
                                )
                        )
                    }
                    res.select("video#player track[kind=captions]").amap {
                        val subLink = it.attr("src")
                        val subLang = it.attr("srclang")
                        subtitleCallback.invoke(SubtitleFile(subLang, subLink))
                    }
                }
                else -> {}
            }
        }
        return true
    }
}
