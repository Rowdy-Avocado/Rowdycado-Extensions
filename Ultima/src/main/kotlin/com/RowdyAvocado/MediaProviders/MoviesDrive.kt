package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName.*
import com.RowdyAvocado.UltimaMediaProvidersUtils.createSlug
import com.RowdyAvocado.UltimaMediaProvidersUtils.getBaseUrl
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.loadExtractor

class MoviesDriveProvider : MediaProvider() {
    override val name = "MoviesDrive"
    override val domain = "https://moviesdrive.online"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val title = data.title
        val season = data.season
        val episode = data.episode
        try {
            val fixTitle = title.createSlug()
            val mediaurl = "$url/$fixTitle"
            val document = app.get(mediaurl).document
            if (season == null) {
                document.select("h5 > a").map {
                    val link = it.attr("href")
                    val urls = ExtractMdrive(link)
                    urls.forEach { servers ->
                        val domain = getBaseUrl(servers)
                        when (domain) {
                            "https://gamerxyt.com" ->
                                    UltimaMediaProvidersUtils.commonLinkLoader(
                                            name,
                                            MDrive,
                                            servers,
                                            null,
                                            null,
                                            subtitleCallback,
                                            callback
                                    )
                        }
                    }
                }
            } else {
                val stag = "Season $season"
                val sep = "Ep$episode"
                val entries = document.select("h5:matches((?i)$stag)")
                entries.apmap { entry ->
                    val href = entry.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""
                    if (href.isNotBlank()) {
                        val doc = app.get(href).document
                        doc.select("h5:matches((?i)$sep)").forEach { epElement ->
                            val linklist = mutableListOf<String>()
                            epElement.nextElementSibling()?.let { sibling ->
                                sibling.selectFirst("h5 > a")?.let { linklist.add(it.attr("href")) }
                                sibling.nextElementSibling()?.let { nextSibling ->
                                    nextSibling.selectFirst("h5 > a")?.let {
                                        linklist.add(it.attr("href"))
                                    }
                                }
                            }
                            linklist.forEach { url ->
                                val links = ExtractMdriveSeries(url)
                                links.forEach { link ->
                                    val domain = getBaseUrl(link)
                                    when (domain) {
                                        "https://gamerxyt.com" ->
                                                UltimaMediaProvidersUtils.commonLinkLoader(
                                                        name,
                                                        MDrive,
                                                        link,
                                                        null,
                                                        null,
                                                        subtitleCallback,
                                                        callback
                                                )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
    }

    suspend fun ExtractMdrive(url: String): MutableList<String> {
        val doc = app.get(url).document
        val linklist = mutableListOf(String())
        doc.select("h5 > a").forEach {
            val link = it.attr("href").replace("lol", "day")
            if (!link.contains("gdtot")) {
                val mainpage =
                        app.get(link)
                                .document
                                .selectFirst("a.btn.btn-primary")
                                ?.attr("href")
                                .toString()
                if (!mainpage.contains("https://")) {
                    val newlink = "https://hubcloud.day$mainpage"
                    linklist.add(newlink)
                } else {
                    linklist.add(mainpage)
                }
            }
        }
        return linklist
    }

    suspend fun ExtractMdriveSeries(url: String): MutableList<String> {
        val linklist = mutableListOf(String())
        val mainpage =
                app.get(url.replace("lol", "day"))
                        .document
                        .selectFirst("a.btn.btn-primary")
                        ?.attr("href")
                        .toString()
        if (!mainpage.contains("https://")) {
            val newlink = "https://hubcloud.day$mainpage"
            linklist.add(newlink)
        } else {
            linklist.add(mainpage)
        }
        return linklist
    }

    // Extractor

    open class Mdrive : ExtractorApi() {
        override val name: String = "Mdrive"
        override val mainUrl: String = "https://gamerxyt.com"
        override val requiresReferer = false

        override suspend fun getUrl(
                url: String,
                referer: String?,
                subtitleCallback: (SubtitleFile) -> Unit,
                callback: (ExtractorLink) -> Unit
        ) {
            val host = url.substringAfter("?").substringBefore("&")
            val id = url.substringAfter("id=").substringBefore("&")
            val token = url.substringAfter("token=").substringBefore("&")
            val Cookie = "$host; hostid=$id; hosttoken=$token"
            val doc = app.get("$mainUrl/games/", headers = mapOf("Cookie" to Cookie)).document
            val links = doc.select("div.card-body > h2 > a").attr("href")
            val header = doc.selectFirst("div.card-header")?.text()
            if (links.contains("pixeldrain")) {
                callback.invoke(
                        ExtractorLink(
                                "MovieDrive",
                                "PixelDrain",
                                links,
                                referer = links,
                                quality = UltimaMediaProvidersUtils.getIndexQuality(header),
                                type = INFER_TYPE
                        )
                )
            } else if (links.contains("gofile")) {
                loadExtractor(links, subtitleCallback, callback)
            } else {
                callback.invoke(
                        ExtractorLink(
                                "MovieDrive",
                                "MovieDrive",
                                links,
                                referer = "",
                                quality = UltimaMediaProvidersUtils.getIndexQuality(header),
                                type = INFER_TYPE
                        )
                )
            }
        }
    }
}
