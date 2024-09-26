package com.example

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.utils.ExtractorLink


class ExtensionName : MainAPI() {
    override var mainUrl = "Domain of Website"
    override var name = "Name Of Website"
    override val hasMainPage = true //if website has Homepage
    override var lang = "en" //Website Language
    override val hasDownloadSupport = true //if Extension support Downloading
    override val supportedTypes =
        setOf(TvType.Cartoon) //Types like Anime,Movie,cartoon,Other,Livestream

    override val mainPage = mainPageOf(
        "website page" to "Category name/name of page",
        //Websites Pages
    )


    //Get the Homepage
    // remove the // before the overide and the curly brace at the bottom when working
  //  override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        /*
        var list = mutableListOf<AnimeSearchResponse>()
        val res = app.get("$mainUrl/${request.data}/page/$page").document
        res.select("article.anime.poster.por").mapNotNull { article ->
            val name = article.selectFirst("header > div.ttl")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("src")
            val url = article.selectFirst("a.lnk-blk")?.attr("href") ?: ""
            list.add(newAnimeSearchResponse(name, url)
            {
                this.posterUrl = poster
            })
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = list,
                isHorizontalImages = true
            ),
            hasNext = true
        )
            */

  //  }



    // This function gets called when you search for something also
    //This is to get Title,Href,Posters for Homepage
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf()
    }


    // This function gets called when you enter the page/show
    override suspend fun load(url: String): LoadResponse? {
        // Depending on source use either
        // return newMovieLoadResponse(name, url, TvType.Movie, url)
        // or use
        // return newAnimeLoadResponse(title, url, TvType.Anime)

        // When you are ready to use the returns above, remove the return null and the ? after the
        // LoadResponse, this was just to avoid an error
        return null
    }

    // This function is how you load the links
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // We use the callback when we are ready to invoke the links
        /*
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = this.name,
                url = sourceurl,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = false
            )
        )
        subtitleCallback.invoke(
            SubtitleFile(
                "eng",
                subtitle
            )
        )
        */
        return true
    }
}
