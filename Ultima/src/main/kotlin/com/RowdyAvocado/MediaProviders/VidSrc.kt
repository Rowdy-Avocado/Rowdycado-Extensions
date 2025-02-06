package com.RowdyAvocado

import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.VidSrcExtractor
import com.lagradost.cloudstream3.utils.ExtractorLink

class VidSrcMediaProvider : MediaProvider() {
    override val name = "VidSrc"
    override val domain = "https://vidsrc.net"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val id = data.tmdbId?.let { "tmdb=$it" } ?: data.imdbId?.let { "imdb=$it" } ?: return
        val iFrameUrl =
                if (data.season == null) {
                    "$url/embed/movie?$id"
                } else {
                    "$url/embed/tv?$id&season=${data.season}&episode=${data.episode}"
                }
        Log.d("rowdy iFrameUrl", iFrameUrl)
        VidSrcExtractor().getUrl(iFrameUrl, url, subtitleCallback, callback)
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    // #endregion - Data classes

}
