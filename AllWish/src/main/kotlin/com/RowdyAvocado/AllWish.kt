package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AllWish(val plugin: AllWishPlugin) : MainAPI() {
    override var mainUrl = AllWish.mainUrl
    override var name = AllWish.name
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val hasMainPage = true

    val mapper = jacksonObjectMapper()
    var sectionNamesList: List<String> = emptyList()

    companion object {
        val mainUrl = "https://all-wish.me"
        var name = "AllWish"
        val xmlHeader = mapOf("X-Requested-With" to "XMLHttpRequest")
        val refHeader = mapOf("Referer" to mainUrl)
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/ajax/home/widget/trending?page=" to "Trending",
                    "$mainUrl/ajax/home/widget/updated-sub?page=" to "Recently Updated (Sub)",
                    "$mainUrl/ajax/home/widget/updated-dub?page=" to "Recently Updated (Dub)",
                    "$mainUrl/ajax/home/widget/hindi-dub?page=" to "Recently Updated (Hindi)",
                    "$mainUrl/ajax/home/widget/updated-china?page=" to "Recently Updated (Chinese)"
            )

    private fun searchResponseBuilder(res: Document): List<AnimeSearchResponse> {
        var results = emptyList<AnimeSearchResponse>()
        res.select("div.item").forEach { item ->
            val name = item.selectFirst("div.name > a")?.text() ?: ""
            val url = item.selectFirst("div.name > a")?.attr("href")?.substringBeforeLast("/") ?: ""
            results +=
                    newAnimeSearchResponse(name, url) {
                        this.posterUrl = item.selectFirst("a.poster img")?.attr("data-src")
                    }
        }
        return results
    }

    override suspend fun search(query: String): List<AnimeSearchResponse> {
        val res = app.get("$mainUrl/filter?keyword=$query").document
        return searchResponseBuilder(res)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res =
                app.get(request.data + page.toString(), AllWish.xmlHeader).parsedSafe<APIResponse>()
        if (res?.status == 200) {
            val searchRes = searchResponseBuilder(res.html)
            return newHomePageResponse(request.name, searchRes, true)
        } else return null
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url).document
        val id = res.select("main > div.container").attr("data-id")
        val data = res.selectFirst("div#media-info")
        val name = data?.selectFirst("h1.title")?.text()?.trim()?.replace(" (Dub)", "") ?: ""
        val posterRegex = Regex("/'(.*)'/gm")

        var subEpisodes = emptyList<Episode>()
        var dubEpisodes = emptyList<Episode>()

        val epRes =
                app.get("$mainUrl/ajax/episode/list/$id", AllWish.xmlHeader)
                        .parsedSafe<APIResponse>()
        if (epRes?.status == 200) {
            epRes.html.select("div.range > div > a").forEach { ep ->
                val epId = ep.attr("data-ids")
                if (ep.attr("data-sub").equals("1"))
                        subEpisodes +=
                                newEpisode("sub|" + epId) {
                                    this.episode = ep.attr("data-num").toFloat().toInt()
                                }
                if (ep.attr("data-dub").equals("1"))
                        dubEpisodes +=
                                newEpisode("softsub,dub|" + epId) {
                                    this.episode = ep.attr("data-num").toFloat().toInt()
                                }
            }
        }

        return newAnimeLoadResponse(name, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, subEpisodes)
            addEpisodes(DubStatus.Dubbed, dubEpisodes)
            this.plot = data?.selectFirst("div.description > div.full > div")?.text()?.trim()
            this.backgroundPosterUrl =
                    posterRegex
                            .find(res.selectFirst("div.media-bg")?.attr("style")!!)
                            ?.destructured
                            ?.toList()
                            ?.get(0)
                            ?: data?.selectFirst("div.poster img")?.attr("src") ?: ""
            this.year =
                    data?.select("div.meta > div > span")
                            ?.find { it.attr("itemprop").equals("dateCreated") }
                            ?.text()
                            ?.toInt()
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val type = data.replace(mainUrl + "/", "").split("|")[0].split(",")
        val id = data.replace(mainUrl + "/", "").split("|")[1]
        val res =
                app.get("$mainUrl/ajax/server/list?servers=$id", AllWish.xmlHeader)
                        .parsedSafe<APIResponse>()
        if (res?.status == 200) {
            res.html.select("div.server-type").forEach { section ->
                if (type.contains(section.attr("data-type"))) {
                    section.select("div.server-list > div.server").forEach { server ->
                        val serverName = server.selectFirst("div > span")?.text() ?: ""
                        val dataId = server.attr("data-link-id")
                        val apiRes =
                                app.get("$mainUrl/ajax/server?get=$dataId", AllWish.xmlHeader)
                                        .parsedSafe<APIResponseUrl>()
                        val realUrl = apiRes?.result?.url ?: ""
                        AllWishExtractor().getUrl(realUrl, serverName, subtitleCallback, callback)
                    }
                }
            }
        }

        return true
    }

    data class APIResponse(
            @JsonProperty("status") val status: Int? = null,
            @JsonProperty("result") val result: String? = null,
            val html: Document = Jsoup.parse(result ?: "")
    )

    data class APIResponseUrl(
            @JsonProperty("status") val status: Int? = null,
            @JsonProperty("result") val result: ServerUrl? = null,
    )

    data class ServerUrl(
            @JsonProperty("url") val url: String? = null,
    )
}
