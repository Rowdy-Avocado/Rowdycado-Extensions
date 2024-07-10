package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlin.text.replace

class MangaDex(val plugin: MangaDexPlugin) : MainAPI() {
    override var name = "MangaDex"
    override var mainUrl = "https://mangadex.org"
    override var supportedTypes = setOf(TvType.Others)
    override var lang = "en"
    override val hasMainPage = true

    var apiUrl = "https://api.mangadex.org"
    var limit = 15
    val mapper = jacksonObjectMapper()

    override suspend fun search(query: String): List<SearchResponse> {
        val url =
                "$apiUrl/manga?title=$query&limit=20&hasAvailableChapters=true&includes[]=cover_art&order[relevance]=desc"
        val res = app.get(url).parsedSafe<MultiMangaResponse>()!!
        if (res.result.equals("ok")) return searchResponseBuilder(res.data)
        else return listOf<SearchResponse>()
    }

    override val mainPage =
            mainPageOf(
                    "$apiUrl/manga?limit=$limit&offset=#&order[createdAt]=desc&includes[]=cover_art&hasAvailableChapters=true" to
                            "Latest Updates",
                    "$apiUrl/manga?limit=$limit&offset=#&order[followedCount]=desc&includes[]=cover_art&hasAvailableChapters=true" to
                            "Popular Titles",
                    "$apiUrl/list/805ba886-dd99-4aa4-b460-4bd7c7b71352" to "Staff Picks",
                    "$apiUrl/list/1cc30d64-45c6-45a6-8c45-3771e1933b0f" to "Seasonal",
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        var url: String = request.data
        if (url.contains("list")) {
            val listResponse = app.get(request.data).parsedSafe<ListResponse>()!!
            if (listResponse.result.equals("ok")) {
                val mangaInList = listResponse.data.rel.joinToString("&ids[]=") { it.id }
                url = "$apiUrl/manga?includes[]=cover_art&ids[]=" + mangaInList
            } else return null
        }
        val res =
                app.get(url.replace("#", ((page - 1) * limit).toString()))
                        .parsedSafe<MultiMangaResponse>()!!
        if (res.result.equals("ok")) {
            return newHomePageResponse(
                    request.name,
                    searchResponseBuilder(res.data),
                    !request.data.contains("list")
            )
        }
        throw ErrorLoadingException("Nothing to show here.")
    }

    override suspend fun load(url: String): LoadResponse {
        val manga =
                app.get("${url.replace(mainUrl, apiUrl)}?includes[]=cover_art")
                        .parsedSafe<SingleMangaResponse>()
                        ?.data!!
        val mangaId = manga.id
        val poster = manga.rel.find { it.type.equals("cover_art") }?.attrs!!.fileName
        val posterUrl = "$mainUrl/covers/$mangaId/$poster"
        var chaptersResponse = emptyList<ChapterData?>()
        var chapterGroup = mutableMapOf<String, MutableList<ChapterData>>()
        var volumeNames = emptySet<SeasonData>()
        var counter = 0
        val limit = 500

        while (counter >= 0) {
            val res =
                    app.get(
                                    "$apiUrl/manga/$mangaId/feed?includes[]=scanlation_group&order[volume]=asc&order[chapter]=asc&limit=$limit&offset=${counter*limit}"
                            )
                            .parsedSafe<ChaptersListResponse>()!!
            chaptersResponse += res.data
            if ((res.limit + res.offset) < res.total) counter += 1 else counter = -1
        }

        chaptersResponse.forEach { chRes ->
            val chNum = chRes!!.attrs.chapter ?: "0"
            if (chNum in chapterGroup) {
                chapterGroup[chNum]!!.add(chRes)
            } else chapterGroup.put(chNum, mutableListOf(chRes))
        }

        val chapters =
                chapterGroup.toSortedMap().mapNotNull { chapter ->
                    newEpisode(chapter.key) {
                        this.name =
                                chapter.value
                                        .filter { it.attrs.translatedLanguage.equals("en") }
                                        .mapNotNull { it.attrs.title }
                                        .filter { it.trim().isNotEmpty() }
                                        .toSet()
                                        .joinToString(" | ")
                                        .ifEmpty { "Chapter " + chapter.key }
                        this.episode = chapter.key.toFloat().toInt()
                        this.season = chapter.value.first().attrs.volume?.toFloat()?.toInt()
                        val volumeNum = this.season ?: 0
                        volumeNames +=
                                if (volumeNum == 0) SeasonData(0, "No Volume")
                                else SeasonData(volumeNum, "Volume " + volumeNum)
                        this.data = mapper.writeValueAsString(chapter.value)
                    }
                }

        return newAnimeLoadResponse(manga.attrs.title.name, url, TvType.CustomMedia) {
            addEpisodes(DubStatus.Dubbed, chapters.sortedBy { it.episode })
            this.seasonNames = volumeNames.toList()
            this.backgroundPosterUrl = posterUrl
            this.posterUrl = posterUrl
            this.plot = manga.attrs.desc.en
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val chapterGroup = AppUtils.parseJson<MutableList<ChapterData>>(data)
        plugin.loadChapterProviders(chapterGroup)
        return false
    }

    private fun searchResponseBuilder(dataList: List<MangaData?>): List<SearchResponse> {
        val searchCollection =
                dataList.mapNotNull { manga ->
                    val mangaId = manga?.id
                    val poster = manga!!.rel.find { it.type.equals("cover_art") }?.attrs!!.fileName
                    val posterUrl = "$mainUrl/covers/$mangaId/$poster"
                    newAnimeSearchResponse(manga.attrs.title.name, "manga/" + manga.id) {
                        this.posterUrl = posterUrl
                    }
                }
        return searchCollection
    }
}

// ======================== Manga API Response ==================================

data class ListResponse(
        @JsonProperty("result") var result: String,
        @JsonProperty("data") var data: ListData
)

data class ListData(@JsonProperty("relationships") var rel: List<MangaInList>)

data class MangaInList(@JsonProperty("id") var id: String)

// ======================== Manga API Response ==================================

data class MultiMangaResponse(
        @JsonProperty("result") var result: String,
        @JsonProperty("data") var data: List<MangaData?>
)

data class SingleMangaResponse(
        @JsonProperty("result") var result: String,
        @JsonProperty("data") var data: MangaData?
)

data class MangaData(
        @JsonProperty("id") var id: String,
        @JsonProperty("attributes") var attrs: MangaAttributes,
        @JsonProperty("relationships") var rel: List<MangaRelationships>,
)

data class MangaAttributes(
        @JsonProperty("title") var title: MangaTitle,
        @JsonProperty("description") var desc: MangaDesc
)

data class MangaTitle(
        @JsonProperty("en") var en: String? = null,
        @JsonProperty("ja") var ja: String? = null,
        @JsonProperty("ja-ro") var jaRo: String? = null,
        var name: String = en ?: ja ?: jaRo ?: ""
)

data class MangaDesc(@JsonProperty("en") var en: String? = null)

data class MangaRelationships(
        @JsonProperty("id") var id: String,
        @JsonProperty("type") var type: String,
        @JsonProperty("attributes") var attrs: MangaRelationshipsAttributes? = null,
)

data class MangaRelationshipsAttributes(
        @JsonProperty("fileName") var fileName: String? = null,
)

// ====================== Chapter API Response ==============================

data class ChaptersListResponse(
        @JsonProperty("result") var result: String,
        @JsonProperty("data") var data: List<ChapterData?>,
        @JsonProperty("limit") var limit: Int,
        @JsonProperty("offset") var offset: Int,
        @JsonProperty("total") var total: Int,
)

data class ChapterData(
        @JsonProperty("id") var id: String,
        @JsonProperty("attributes") var attrs: ChapterAttributes,
        @JsonProperty("relationships") var rel: List<ChapterRelationships>,
)

data class ChapterAttributes(
        @JsonProperty("title") var title: String? = null,
        @JsonProperty("pages") var pages: Int? = null,
        @JsonProperty("volume") var volume: String? = null,
        @JsonProperty("chapter") var chapter: String? = null,
        @JsonProperty("externalUrl") var externalUrl: String? = null,
        @JsonProperty("translatedLanguage") var translatedLanguage: String? = null,
)

data class ChapterRelationships(
        @JsonProperty("id") var id: String,
        @JsonProperty("type") var type: String,
        @JsonProperty("attributes") var attrs: ChapterScanlationGroupAttributes? = null,
)

data class ChapterScanlationGroupAttributes(
        @JsonProperty("name") var name: String? = null,
)
