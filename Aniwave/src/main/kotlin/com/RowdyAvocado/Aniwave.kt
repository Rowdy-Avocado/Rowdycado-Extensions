package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDubStatus
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.AnyVidplay
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URLEncoder
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@OptIn(kotlin.ExperimentalStdlibApi::class)
class Aniwave : MainAPI() {
    override var mainUrl = AniwavePlugin.currentAniwaveServer
    override var name = "Aniwave/9Anime"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedSyncNames = setOf(SyncIdName.Anilist, SyncIdName.MyAnimeList)
    override val supportedTypes = setOf(TvType.Anime)
    override val hasQuickSearch = true

    companion object {
        var keys: Pair<String, String>? = null

        fun encode(input: String): String =
                java.net.URLEncoder.encode(input, "utf-8").replace("+", "%2B")

        private fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")
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

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst(".info .name") ?: return null
        val link = title.attr("href").replace(Regex("/ep.*\$"), "")
        val poster = this.selectFirst(".poster > a > img")?.attr("src")
        val meta = this.selectFirst(".poster > a > .meta > .inner > .left")
        val subbedEpisodes = meta?.selectFirst(".sub")?.text()?.toIntOrNull()
        val dubbedEpisodes = meta?.selectFirst(".dub")?.text()?.toIntOrNull()

        return newAnimeSearchResponse(title.text() ?: return null, link) {
            this.posterUrl = poster
            addDubStatus(
                    dubbedEpisodes != null,
                    subbedEpisodes != null,
                    dubbedEpisodes,
                    subbedEpisodes
            )
        }
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/ajax/home/widget/trending?page=" to "Trending",
                    "$mainUrl/ajax/home/widget/updated-all?page=" to "All",
                    "$mainUrl/ajax/home/widget/updated-sub?page=" to "Recently Updated (SUB)",
                    "$mainUrl/ajax/home/widget/updated-dub?page=" to "Recently Updated (DUB)",
                    "$mainUrl/ajax/home/widget/updated-china?page=" to "Recently Updated (Chinese)",
                    "$mainUrl/ajax/home/widget/random?page=" to "Random",
            )

    override suspend fun quickSearch(query: String): List<SearchResponse> {
        delay(1000)
        return search(query)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/filter?keyword=${query}"
        return app.get(url).document.select("#list-items div.inner:has(div.poster)").mapNotNull {
            it.toSearchResponse()
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val res = app.get(url).parsed<Response>()
        if (!res.status.equals(200)) throw ErrorLoadingException("Could not connect to the server")
        val home = res.getHtml().select("div.item").mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, home, true)
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val meta = doc.selectFirst("#w-info") ?: throw ErrorLoadingException("Could not find info")
        val ratingElement = meta.selectFirst(".brating > #w-rating")
        val id = ratingElement?.attr("data-id") ?: throw ErrorLoadingException("Could not find id")
        val binfo =
                meta.selectFirst(".binfo") ?: throw ErrorLoadingException("Could not find binfo")
        val info = binfo.selectFirst(".info") ?: throw ErrorLoadingException("Could not find info")
        val poster = binfo.selectFirst(".poster > span > img")?.attr("src")
        val backimginfo = doc.selectFirst("#player")?.attr("style")
        val backimgRegx = Regex("(http|https).*jpg")
        val backposter = backimgRegx.find(backimginfo.toString())?.value ?: poster
        val title =
                (info.selectFirst(".title") ?: info.selectFirst(".d-title"))?.text()
                        ?: throw ErrorLoadingException("Could not find title")
        val vrf = AniwaveUtils.vrfEncrypt(getKeys().first, id)
        val episodeListUrl = "$mainUrl/ajax/episode/list/$id?$vrf"
        val body =
                app.get(episodeListUrl).parsedSafe<Response>()?.getHtml()
                        ?: throw ErrorLoadingException(
                                "Could not parse json with Vrf=$vrf id=$id url=\n$episodeListUrl"
                        )

        val subEpisodes = ArrayList<Episode>()
        val dubEpisodes = ArrayList<Episode>()
        val softsubeps = ArrayList<Episode>()
        val uncensored = ArrayList<Episode>()
        val genres =
                doc.select("div.meta:nth-child(1) > div:contains(Genres:) a").mapNotNull {
                    it.text()
                }
        val recss =
                doc.select("div#watch-second .w-side-section div.body a.item").mapNotNull { rec ->
                    val href = rec.attr("href")
                    val rectitle = rec.selectFirst(".name")?.text() ?: ""
                    val recimg = rec.selectFirst("img")?.attr("src")
                    newAnimeSearchResponse(rectitle, fixUrl(href)) { this.posterUrl = recimg }
                }
        val status =
                when (doc.selectFirst("div.meta:nth-child(1) > div:contains(Status:) span")?.text()
                ) {
                    "Releasing" -> ShowStatus.Ongoing
                    "Completed" -> ShowStatus.Completed
                    else -> null
                }

        val typetwo =
                when (doc.selectFirst("div.meta:nth-child(1) > div:contains(Type:) span")?.text()) {
                    "OVA" -> TvType.OVA
                    "SPECIAL" -> TvType.OVA
                    else -> TvType.Anime
                }
        val duration = doc.selectFirst(".bmeta > div > div:contains(Duration:) > span")?.text()

        body.select(".episodes > ul > li > a").apmap { element ->
            val ids = element.attr("data-ids").split(",", limit = 3)
            val dataDub = element.attr("data-dub").toIntOrNull()
            val epNum = element.attr("data-num").toIntOrNull()
            val epTitle = element.selectFirst("span.d-title")?.text()
            val isUncen = element.attr("data-slug").contains("uncen")

            if (ids.size > 0) {
                if (isUncen) {
                    ids.getOrNull(0)?.let { uncen ->
                        val epdd = "{\"ID\":\"$uncen\",\"type\":\"sub\"}"
                        uncensored.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -4
                                }
                        )
                    }
                } else {
                    if (ids.size == 1 && dataDub == 1) {
                        ids.getOrNull(0)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                    newEpisode(epdd) {
                                        this.episode = epNum
                                        this.name = epTitle
                                        this.season = -2
                                    }
                            )
                        }
                    } else {
                        ids.getOrNull(0)?.let { sub ->
                            val epdd = "{\"ID\":\"$sub\",\"type\":\"sub\"}"
                            subEpisodes.add(
                                    newEpisode(epdd) {
                                        this.episode = epNum
                                        this.name = epTitle
                                        this.season = -1
                                    }
                            )
                        }
                    }
                }
                if (ids.size > 1) {
                    if (dataDub == 0 || ids.size > 2) {
                        ids.getOrNull(1)?.let { softsub ->
                            val epdd = "{\"ID\":\"$softsub\",\"type\":\"softsub\"}"
                            softsubeps.add(
                                    newEpisode(epdd) {
                                        this.episode = epNum
                                        this.name = epTitle
                                        this.season = -3
                                    }
                            )
                        }
                    } else {
                        ids.getOrNull(1)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                    newEpisode(epdd) {
                                        this.episode = epNum
                                        this.name = epTitle
                                        this.season = -2
                                    }
                            )
                        }
                    }

                    if (ids.size > 2) {
                        ids.getOrNull(2)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                    newEpisode(epdd) {
                                        this.episode = epNum
                                        this.name = epTitle
                                        this.season = -2
                                    }
                            )
                        }
                    }
                }
            }
        }

        // season -1 HARDSUBBED
        // season -2 Dubbed
        // Season -3 SofSubbed

        val names =
                listOf(
                        Pair("Sub", -1),
                        Pair("Dub", -2),
                        Pair("S-Sub", -3),
                        Pair("Uncensored", -4),
                )

        // Reading info from web page to fetch anilistData
        val titleRomaji =
                (info.selectFirst(".title") ?: info.selectFirst(".d-title"))?.attr("data-jp") ?: ""
        val premieredDetails =
                info.select(".bmeta > .meta > div")
                        .find { it.text().contains("Premiered: ", true) }
                        ?.selectFirst("span > a")
                        ?.text()
                        ?.split(" ")
        val season = premieredDetails?.get(0).toString()
        val year = premieredDetails?.get(1)?.toInt() ?: 0

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, dubEpisodes)
            addEpisodes(DubStatus.Subbed, subEpisodes)
            addEpisodes(DubStatus.Subbed, softsubeps)
            addEpisodes(DubStatus.Subbed, uncensored)
            this.seasonNames = names.map { (name, int) -> SeasonData(int, name) }
            plot = info.selectFirst(".synopsis > .shorting > .content")?.text()
            this.posterUrl = poster
            rating = ratingElement.attr("data-score").toFloat().times(1000f).toInt()
            this.backgroundPosterUrl = backposter
            this.tags = genres
            this.recommendations = recss
            this.showStatus = status
            if (AniwavePlugin.aniwaveSimklSync)
                    addAniListId(aniAPICall(AniwaveUtils.aniQuery(titleRomaji, year, season))?.id)
            else this.type = typetwo
            addDuration(duration)
        }
    }

    private fun serverName(serverID: String?): String? {
        val sss =
                when (serverID) {
                    "41" -> "vidplay"
                    "44" -> "filemoon"
                    "40" -> "streamtape"
                    "35" -> "mp4upload"
                    "28" -> "MyCloud"
                    else -> null
                }
        return sss
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parseData = AppUtils.parseJson<SubDubInfo>(data)
        val datavrf = AniwaveUtils.vrfEncrypt(getKeys().first, parseData.ID)
        val one = app.get("$mainUrl/ajax/server/list/${parseData.ID}?$datavrf").parsed<Response>()
        val two = one.getHtml()
        val aas =
                two.select("div.servers .type[data-type=${parseData.type}] li").mapNotNull {
                    val datalinkId = it.attr("data-link-id")
                    val serverID = it.attr("data-sv-id").toString()
                    val newSname = serverName(serverID)
                    Pair(newSname, datalinkId)
                }
        aas.amap { (sName, sId) ->
            try {
                val vrf = AniwaveUtils.vrfEncrypt(getKeys().first, sId)
                val videncrr = app.get("$mainUrl/ajax/server/$sId?$vrf").parsed<Links>()
                val encUrl = videncrr.result?.url ?: return@amap
                val asss = AniwaveUtils.vrfDecrypt(getKeys().second, encUrl)

                if (sName.equals("filemoon")) {
                    val res = app.get(asss)
                    if (res.code == 200) {
                        val packedJS =
                                res.document
                                        .selectFirst("script:containsData(function(p,a,c,k,e,d))")
                                        ?.data()
                                        .toString()
                        JsUnpacker(packedJS).unpack().let { unPacked ->
                            Regex("sources:\\[\\{file:\"(.*?)\"")
                                    .find(unPacked ?: "")
                                    ?.groupValues
                                    ?.get(1)
                                    ?.let { link ->
                                        callback.invoke(
                                                ExtractorLink(
                                                        "Filemoon",
                                                        "Filemoon",
                                                        link,
                                                        "",
                                                        Qualities.Unknown.value,
                                                        link.contains(".m3u8")
                                                )
                                        )
                                    }
                        }
                    }
                } else if (sName.equals("vidplay")) {
                    val host = AniwaveUtils.getBaseUrl(asss)
                    AnyVidplay(host).getUrl(asss, host, subtitleCallback, callback)
                } else loadExtractor(asss, subtitleCallback, callback)
            } catch (e: Exception) {}
        }
        return true
    }

    override suspend fun getLoadUrl(name: SyncIdName, id: String): String? {
        val syncId = id.split("/").last()

        // formatting the JSON response to search on aniwave site
        val anilistData = aniAPICall(AniwaveUtils.aniQuery(name, syncId.toInt()))
        val title = anilistData?.title?.romaji ?: anilistData?.title?.english
        val year = anilistData?.year
        val season = anilistData?.season
        val searchUrl =
                "$mainUrl/filter?keyword=${title}&year%5B%5D=${year}&season%5B%5D=unknown&season%5B%5D=${season?.lowercase()}&sort=recently_updated"

        // searching the anime on aniwave site using advance filter and capturing the url from
        // search result
        val document = app.get(searchUrl).document
        val syncUrl =
                document.select("#list-items div.info div.b1 > a")
                        .find { it.attr("data-jp").equals(title, true) }
                        ?.attr("href")
        return fixUrl(syncUrl ?: return null)
    }

    private suspend fun aniAPICall(query: String): Media? {
        // Fetching data using POST method
        val url = "https://graphql.anilist.co"
        val res =
                app.post(
                                url,
                                headers =
                                        mapOf(
                                                "Accept" to "application/json",
                                                "Content-Type" to "application/json",
                                        ),
                                data =
                                        mapOf(
                                                "query" to query,
                                        )
                        )
                        .parsedSafe<SyncInfo>()

        return res?.data?.media
    }

    // JSON formatter for data fetched from anilistApi
    data class SyncTitle(
            @JsonProperty("romaji") val romaji: String? = null,
            @JsonProperty("english") val english: String? = null,
    )

    data class Media(
            @JsonProperty("title") val title: SyncTitle? = null,
            @JsonProperty("id") val id: Int? = null,
            @JsonProperty("idMal") val idMal: Int? = null,
            @JsonProperty("season") val season: String? = null,
            @JsonProperty("seasonYear") val year: Int? = null,
    )

    data class Response(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: String
    ) {
        fun getHtml(): Document {
            return Jsoup.parse(result)
        }
    }

    data class AniwaveMediaInfo(
            @JsonProperty("result") val result: AniwaveResult? = AniwaveResult()
    )

    data class AniwaveResult(
            @JsonProperty("sources") var sources: ArrayList<AniwaveTracks> = arrayListOf(),
            @JsonProperty("tracks") var tracks: ArrayList<AniwaveTracks> = arrayListOf()
    )

    data class AniwaveTracks(
            @JsonProperty("file") var file: String? = null,
            @JsonProperty("label") var label: String? = null,
    )

    data class Data(
            @JsonProperty("Media") val media: Media? = null,
    )

    data class SyncInfo(
            @JsonProperty("data") val data: Data? = null,
    )

    data class Result(@JsonProperty("url") val url: String? = null)

    data class Links(@JsonProperty("result") val result: Result? = null)

    data class SubDubInfo(
            @JsonProperty("ID") val ID: String,
            @JsonProperty("type") val type: String
    )

    data class Keys(@JsonProperty("aniwave") val keys: List<String>)
}
