package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDubStatus
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.util.EnumSet
import org.jsoup.Jsoup

class AnitakuProvider : MainAPI() {
    companion object {
        val mainUrl = "https://anitaku.to"
        val name = "Anitaku"

        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Special")) TvType.OVA
            else if (t.contains("Movie")) TvType.AnimeMovie else TvType.Anime
        }

        fun getStatus(t: String): ShowStatus {
            return when (t) {
                "Completed" -> ShowStatus.Completed
                "Ongoing" -> ShowStatus.Ongoing
                else -> ShowStatus.Completed
            }
        }
    }

    override var mainUrl = AnitakuProvider.mainUrl
    override var name = AnitakuProvider.name
    override val hasQuickSearch = true
    override val hasMainPage = true

    override val supportedTypes =
            setOf(
                    TvType.AnimeMovie,
                    TvType.Anime,
                    TvType.OVA,
            )

    val headers =
            mapOf(
                    "authority" to "ajax.gogocdn.net",
                    "sec-ch-ua" to
                            "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"",
                    "accept" to "text/html, */*; q=0.01",
                    "dnt" to "1",
                    "sec-ch-ua-mobile" to "?0",
                    "user-agent" to
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36",
                    "origin" to mainUrl,
                    "sec-fetch-site" to "cross-site",
                    "sec-fetch-mode" to "cors",
                    "sec-fetch-dest" to "empty",
                    "referer" to "$mainUrl/"
            )
    val parseRegex =
            Regex(
                    "<li>\\s*\n.*\n.*<a\\s*href=[\"'](.*?-episode-(\\d+))[\"']\\s*title=[\"'](.*?)[\"']>\n.*?img src=\"(.*?)\""
            )

    override val mainPage =
            mainPageOf(
                    Pair("1", "Recent Release - Sub"),
                    Pair("2", "Recent Release - Dub"),
                    Pair("3", "Recent Release - Chinese"),
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val params = mapOf("page" to page.toString(), "type" to request.data)
        val html =
                app.get(
                        "https://ajax.gogocdn.net/ajax/page-recent-release.html",
                        headers = headers,
                        params = params
                )
        val isSub = listOf(1, 3).contains(request.data.toInt())

        val home =
                parseRegex
                        .findAll(html.text)
                        .map {
                            val (link, epNum, title, poster) = it.destructured
                            newAnimeSearchResponse(title.replace(" (Dub)", ""), link) {
                                this.posterUrl = poster
                                addDubStatus(!isSub, epNum.toIntOrNull())
                            }
                        }
                        .toList()

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): ArrayList<SearchResponse> {
        val link = "$mainUrl/search.html?keyword=$query"
        val html = app.get(link).text
        val doc = Jsoup.parse(html)

        val episodes =
                doc.select(""".last_episodes li""").mapNotNull {
                    newAnimeSearchResponse(
                            name = it.selectFirst(".name")?.text()?.replace(" (Dub)", "")
                                            ?: return@mapNotNull null,
                            url =
                                    fixUrl(
                                            it.selectFirst(".name > a")?.attr("href")
                                                    ?: return@mapNotNull null
                                    ),
                            type = TvType.Anime,
                            fix = true
                    ) {
                        posterUrl = it.selectFirst("img")?.attr("src")
                        year =
                                it.selectFirst(".released")
                                        ?.text()
                                        ?.split(":")
                                        ?.getOrNull(1)
                                        ?.trim()
                                        ?.toIntOrNull()
                        dubStatus =
                                if (it.selectFirst(".name")?.text()?.contains("Dub") == true) {
                                    EnumSet.of(DubStatus.Dubbed)
                                } else {
                                    EnumSet.of(DubStatus.Subbed)
                                }
                    }
                }

        return ArrayList(episodes)
    }

    private fun getProperAnimeLink(uri: String): String {
        if (uri.contains("-episode")) {
            val split = uri.split("/")
            val slug = split[split.size - 1].split("-episode")[0]
            return "$mainUrl/category/$slug"
        }
        return uri
    }

    override suspend fun load(url: String): LoadResponse {
        val link = getProperAnimeLink(url)
        val isDub = url.contains("-dub")
        val episodeloadApi = "https://ajax.gogocdn.net/ajax/load-list-episode"
        val doc = app.get(link).document

        val animeBody = doc.selectFirst(".anime_info_body_bg")
        val title = animeBody?.selectFirst("h1")!!.text().replace(" (Dub)", "")
        val poster = animeBody.selectFirst("img")?.attr("src")
        var description: String? = null
        val genre = ArrayList<String>()
        var year: Int? = null
        var status: String? = null
        var nativeName: String? = null
        var type: String? = null

        animeBody.select("p.type").forEach { pType ->
            when (pType.selectFirst("span")?.text()?.trim()) {
                "Plot Summary:" -> {
                    description = pType.text().replace("Plot Summary:", "").trim()
                }
                "Genre:" -> {
                    genre.addAll(pType.select("a").map { it.attr("title") })
                }
                "Released:" -> {
                    year = pType.text().replace("Released:", "").trim().toIntOrNull()
                }
                "Status:" -> {
                    status = pType.text().replace("Status:", "").trim()
                }
                "Other name:" -> {
                    nativeName = pType.text().replace("Other name:", "").trim()
                }
                "Type:" -> {
                    type = pType.text().replace("type:", "").trim()
                }
            }
        }

        val animeId = doc.selectFirst("#movie_id")!!.attr("value")
        val params = mapOf("ep_start" to "0", "ep_end" to "2000", "id" to animeId)

        val episodes =
                app.get(episodeloadApi, params = params)
                        .document
                        .select("a")
                        .map {
                            Episode(
                                    fixUrl(it.attr("href").trim()),
                                    "Episode " +
                                            it.selectFirst(".name")
                                                    ?.text()
                                                    ?.replace("EP", "")
                                                    ?.trim()
                            )
                        }
                        .reversed()

        var altEpisodes = emptyList<Episode>()
        val altLink = if (isDub) link.removeSuffix("-dub") else link + "-dub"
        val altDoc = app.get(altLink).document
        if (altDoc.selectFirst("h1.entry-title") == null) {
            val dubAnimeId = altDoc.selectFirst("#movie_id")!!.attr("value")
            val dubParams = mapOf("ep_start" to "0", "ep_end" to "2000", "id" to dubAnimeId)
            altEpisodes =
                    app.get(episodeloadApi, params = dubParams)
                            .document
                            .select("a")
                            .map {
                                Episode(
                                        fixUrl(it.attr("href").trim()),
                                        "Episode " +
                                                it.selectFirst(".name")
                                                        ?.text()
                                                        ?.replace("EP", "")
                                                        ?.trim()
                                )
                            }
                            .reversed()
        }

        val mainDubStatus = if (isDub) DubStatus.Dubbed else DubStatus.Subbed
        val altDubStatus = if (isDub) DubStatus.Subbed else DubStatus.Dubbed

        return newAnimeLoadResponse(title, link, getType(type.toString())) {
            japName = nativeName
            engName = title
            posterUrl = poster
            this.year = year
            addEpisodes(mainDubStatus, episodes) // TODO CHECK
            if (altEpisodes.isNotEmpty()) addEpisodes(altDubStatus, altEpisodes)
            plot = description
            tags = genre

            showStatus = getStatus(status.toString())
        }
    }

    data class GogoSources(
            @JsonProperty("source") val source: List<GogoSource>?,
            @JsonProperty("sourceBk") val sourceBk: List<GogoSource>?,
    )

    data class GogoSource(
            @JsonProperty("file") val file: String,
            @JsonProperty("label") val label: String?,
            @JsonProperty("type") val type: String?,
            @JsonProperty("default") val default: String? = null
    )

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("div.anime_muti_link > ul > li").mapNotNull { item ->
            val serverName = item.attr("class")
            val url = item.selectFirst("a")?.attr("data-video")
            AnitakuExtractor().getUrl(url ?: "", serverName, subtitleCallback, callback)
        }
        return true
    }

    private fun serverName(sId: String): String {
        when (sId) {
            "streamwish" -> return "Streamwish"
            "filelions" -> return "FileLions"
            "anime" -> return "Vidstreaming"
        }
        return ""
    }

    // private suspend fun linksFromScript(url:String?): List<String> {
    //     var links = emptyList<String>()
    //     if(!url.isNullOrBlank()) {
    //         val serverRes = app.get(url)
    //         if (serverRes.code == 200) {
    //             val doc = serverRes.document
    //             val script = doc.selectFirst("script:containsData(sources)")?.data().toString()
    //             Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
    //                 links += link
    //             }
    //         }
    //     }
    //     return links
    // }
}
