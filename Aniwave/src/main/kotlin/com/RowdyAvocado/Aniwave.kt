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
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDubStatus
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import kotlin.io.encoding.Base64
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
class Aniwave : MainAPI() {
    override var mainUrl = AniwavePlugin.currentAniwaveServer
    override var name = "Aniwave/9Anime"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedSyncNames = setOf(SyncIdName.Anilist, SyncIdName.MyAnimeList)
    override val supportedTypes = setOf(TvType.Anime)
    override val hasQuickSearch = true
    private val xmlHeader = mapOf("x-requested-with" to "XMLHttpRequest")

    companion object {
        var keys: Keys? = null

        fun encode(input: String): String =
                java.net.URLEncoder.encode(input, "utf-8").replace("+", "%2B")

        private fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")
    }

    private suspend fun getKeys(): Keys {
        return Keys(listOf(Step(1, "")))
        // if (keys == null) {
        //     keys =
        //             app.get("https://rowdy-avocado.github.io/multi-keys/").parsedSafe<Keys>()
        //                     ?: throw Exception("Unable to fetch keys")
        // }
        // return keys!!
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
                    "$mainUrl/ajax/home/widget/updated-all?page=" to "Recently Updated",
                    "$mainUrl/ajax/home/widget/trending?page=" to "Trending",
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
        val res = app.get(url, headers = xmlHeader).parsed<Response>()
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
        // val vrf = AniwaveUtils.vrfEncrypt(getKeys(), id)
        // val episodeListUrl = "$mainUrl/ajax/episode/list/$id?$vrf"
        val episodeListUrl = "$mainUrl/ajax/episode/list/$id"
        val body =
                app.get(episodeListUrl, headers = xmlHeader).parsedSafe<Response>()?.getHtml()
                        ?: throw ErrorLoadingException(
                                // "Could not parse json with Vrf=$vrf id=$id url=\n$episodeListUrl"
                                "Could not parse json with id=$id url=\n$episodeListUrl"
                        )

        val subEpisodes = ArrayList<Episode>()
        val dubEpisodes = ArrayList<Episode>()
        // val softsubeps = ArrayList<Episode>()
        // val uncensored = ArrayList<Episode>()

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
            // val ids = element.attr("data-ids").split(",", limit = 3)
            // val epTitle = element.selectFirst("span.d-title")?.text()
            // val isUncen = element.attr("data-slug").contains("uncen")
            val ids = element.attr("data-ids")
            val dataSub = element.attr("data-sub").toIntOrNull() ?: 0
            val dataDub = element.attr("data-dub").toIntOrNull() ?: 0
            val epNum = element.attr("data-num").toIntOrNull()

            if (dataSub.equals(1)) {
                val epdd = "{\"ID\":\"$ids\",\"type\":\"sub\"}"
                subEpisodes.add(newEpisode(epdd) { this.episode = epNum })
            }

            if (dataDub.equals(1)) {
                val epdd = "{\"ID\":\"$ids\",\"type\":\"dub\"}"
                dubEpisodes.add(newEpisode(epdd) { this.episode = epNum })
            }

            // if (ids.size > 0) {
            //     if (isUncen) {
            //         ids.getOrNull(0)?.let { uncen ->
            //             val epdd = "{\"ID\":\"$uncen\",\"type\":\"sub\"}"
            //             uncensored.add(
            //                     newEpisode(epdd) {
            //                         this.episode = epNum
            //                         this.name = epTitle
            //                         this.season = -4
            //                     }
            //             )
            //         }
            //     } else {
            //         if (ids.size == 1 && dataDub == 1) {
            //             ids.getOrNull(0)?.let { dub ->
            //                 val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
            //                 dubEpisodes.add(
            //                         newEpisode(epdd) {
            //                             this.episode = epNum
            //                             this.name = epTitle
            //                             this.season = -2
            //                         }
            //                 )
            //             }
            //         } else {
            //             ids.getOrNull(0)?.let { sub ->
            //                 val epdd = "{\"ID\":\"$sub\",\"type\":\"sub\"}"
            //                 subEpisodes.add(
            //                         newEpisode(epdd) {
            //                             this.episode = epNum
            //                             this.name = epTitle
            //                             this.season = -1
            //                         }
            //                 )
            //             }
            //         }
            //     }
            //     if (ids.size > 1) {
            //         if (dataDub == 0 || ids.size > 2) {
            //             ids.getOrNull(1)?.let { softsub ->
            //                 val epdd = "{\"ID\":\"$softsub\",\"type\":\"softsub\"}"
            //                 softsubeps.add(
            //                         newEpisode(epdd) {
            //                             this.episode = epNum
            //                             this.name = epTitle
            //                             this.season = -3
            //                         }
            //                 )
            //             }
            //         } else {
            //             ids.getOrNull(1)?.let { dub ->
            //                 val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
            //                 dubEpisodes.add(
            //                         newEpisode(epdd) {
            //                             this.episode = epNum
            //                             this.name = epTitle
            //                             this.season = -2
            //                         }
            //                 )
            //             }
            //         }

            //         if (ids.size > 2) {
            //             ids.getOrNull(2)?.let { dub ->
            //                 val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
            //                 dubEpisodes.add(
            //                         newEpisode(epdd) {
            //                             this.episode = epNum
            //                             this.name = epTitle
            //                             this.season = -2
            //                         }
            //                 )
            //             }
            //         }
            //     }
            // }
        }

        // season -1 HARDSUBBED
        // season -2 Dubbed
        // Season -3 SofSubbed

        // val names =
        //         listOf(
        //                 Pair("Sub", -1),
        //                 Pair("Dub", -2),
        //                 Pair("S-Sub", -3),
        //                 Pair("Uncensored", -4),
        //         )

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
            // addEpisodes(DubStatus.Subbed, softsubeps)
            // addEpisodes(DubStatus.Subbed, uncensored)
            // this.seasonNames = names.map { (name, int) -> SeasonData(int, name) }
            addEpisodes(DubStatus.Dubbed, dubEpisodes)
            addEpisodes(DubStatus.Subbed, subEpisodes)
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
                    "0f2" -> "vidplay" // working
                    "089" -> "mycloud" // working
                    "5c2" -> "vidstreaming" // not working
                    "224" -> "gogo" // not working
                    "5c3" -> "streamwish" // working
                    "f64" -> "mp4upload" // working
                    "731" -> "doodstream" // working
                    "478" -> "vidhide" // working
                    "c4f" -> "filelions" // working
                    "323" -> "zoro" // working
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
        // val datavrf = AniwaveUtils.vrfEncrypt(getKeys(), parseData.ID)
        val one =
                app.get("$mainUrl/ajax/server/list?servers=${parseData.ID}", headers = xmlHeader)
                        .parsed<Response>()
        val two = one.getHtml()
        val aas =
                two.select("div.servers .type[data-type=${parseData.type}] li.ep-server-item")
                        .mapNotNull {
                            val datalinkId = it.attr("data-link-id")
                            val serverID = it.attr("data-sv-id")
                            val newSname = serverName(serverID) ?: return@mapNotNull null
                            Pair(newSname, datalinkId)
                        }

        aas.amap { (sName, sId) ->
            try {
                val linkRes =
                        app.get("$mainUrl/ajax/server?get=$sId", headers = xmlHeader)
                                .parsed<Links>()
                val link = linkRes.result?.url ?: return@amap
                val host = AniwaveUtils.getBaseUrl(link)
                when (sName) {
                    "vidplay" -> {
                        val iFramePage = app.get(link, referer = host).document
                        val jsData =
                                iFramePage.selectFirst("script:containsData(jwplayer)")
                                        ?: return@amap
                        val fileLink =
                                Regex("""file": `(.*)`""").find(jsData.html())?.groupValues?.get(1)
                                        ?: return@amap
                        callback.invoke(
                                ExtractorLink(
                                        "Vidplay",
                                        "Vidplay",
                                        fileLink,
                                        "",
                                        Qualities.Unknown.value,
                                        fileLink.contains(".m3u8")
                                )
                        )
                    }
                    "mycloud" -> {
                        val encIFrameUrl = app.get(link).url.split("#").getOrNull(1) ?: return@amap
                        val fileLink = Base64.UrlSafe.decode(encIFrameUrl).toString(Charsets.UTF_8)
                        callback.invoke(
                                ExtractorLink(
                                        "Mycloud",
                                        "Mycloud",
                                        fileLink,
                                        "",
                                        Qualities.Unknown.value,
                                        fileLink.contains(".m3u8")
                                )
                        )
                    }
                    "vidstreaming" -> {
                        val iv = "3134003223491201"
                        val secretKey = "37911490979715163134003223491201"
                        val secretDecryptKey = "54674138327930866480207815084989"
                        GogoHelper.extractVidstream(
                                link,
                                "Vidstreaming",
                                callback,
                                iv,
                                secretKey,
                                secretDecryptKey,
                                isUsingAdaptiveKeys = false,
                                isUsingAdaptiveData = true
                        )
                    }
                    "gogo" -> {}
                    "streamwish" ->
                            AnyStreamWish(host).getUrl(link, null, subtitleCallback, callback)
                    "mp4upload" -> AnyMp4Upload(host).getUrl(link, host, subtitleCallback, callback)
                    "doodstream" ->
                            AnyDoodStream(host).getUrl(link, host, subtitleCallback, callback)
                    "vidhide" -> AnyVidhide(host).getUrl(link, host, subtitleCallback, callback)
                    "filelions" -> AnyFilelions(host).getUrl(link, host, subtitleCallback, callback)
                    "zoro" -> {
                        val iFramePage = app.get(link, referer = host).document
                        val jsData =
                                iFramePage
                                        .selectFirst("script:containsData(JsonData)")
                                        ?.html()
                                        ?.split("=")
                                        ?.getOrNull(1)
                                        ?: return@amap
                        val jsonData = AppUtils.parseJson<ZoroJson>(jsData)
                        jsonData.subtitles.amap { sub ->
                            sub.lang?.let { subtitleCallback.invoke(SubtitleFile(it, sub.file)) }
                        }
                        jsonData.sources.amap { source ->
                            callback.invoke(
                                    ExtractorLink(
                                            "Zoro",
                                            "Zoro",
                                            source.file,
                                            "",
                                            Qualities.Unknown.value,
                                            source.file.contains(".m3u8")
                                    )
                            )
                        }
                    }
                    else -> {}
                }
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

    data class Keys(@JsonProperty("aniwave") val aniwave: List<Step>)

    data class Step(
            @JsonProperty("sequence") val sequence: Int,
            @JsonProperty("method") val method: String,
            @JsonProperty("keys") val keys: List<String>? = null
    )

    data class ZoroJson(
            @JsonProperty("tracks") val subtitles: List<Subtitle>,
            @JsonProperty("sources") val sources: List<Source>
    )

    data class Subtitle(
            @JsonProperty("file") val file: String,
            @JsonProperty("label") val lang: String? = null,
            @JsonProperty("kind") val kind: String,
    )

    data class Source(
            @JsonProperty("file") val file: String,
            @JsonProperty("type") val type: String
    )

    class AnyMp4Upload(domain: String = "") : Mp4Upload() {
        override var mainUrl = domain
        override var requiresReferer = false
    }

    class AnyStreamWish(domain: String = "") : StreamWishExtractor() {
        override var mainUrl = domain
    }

    class AnyVidhide(domain: String = "") : VidhideExtractor() {
        override var name = "Vidhide"
        override var mainUrl = domain
        override val requiresReferer = false
    }

    class AnyFilelions(domain: String = "") : VidhideExtractor() {
        override var name = "Filelions"
        override var mainUrl = domain
        override val requiresReferer = false
    }

    open class AnyDoodStream(domain: String) : ExtractorApi() {
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
