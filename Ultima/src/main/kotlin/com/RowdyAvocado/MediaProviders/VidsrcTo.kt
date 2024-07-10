package com.RowdyAvocado

import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.VidSrcTo
import com.lagradost.cloudstream3.utils.ExtractorLink

class VidsrcToMediaProvider : MediaProvider() {
    override val name = "VidsrcTo"
    override val domain = "https://vidsrc.to"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val id = data.tmdbId ?: data.imdbId ?: return
        val iFrameUrl =
                if (data.season == null) {
                    "$url/embed/movie/$id"
                } else {
                    "$url/embed/tv/$id/${data.season}/${data.episode}"
                }
        VidSrcTo().getUrl(iFrameUrl, url, subtitleCallback, callback)
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    // #endregion - Data classes

}

// class VidSrcTo : ExtractorApi() {
//     override val name = "VidSrcTo"
//     override val mainUrl = "https://vidsrc.to"
//     override val requiresReferer = true

//     override suspend fun getUrl(
//             url: String,
//             referer: String?,
//             subtitleCallback: (SubtitleFile) -> Unit,
//             callback: (ExtractorLink) -> Unit
//     ) {
//         val mediaId =
//                 app.get(url).document.selectFirst("ul.episodes li a")?.attr("data-id") ?: return
//         val res =
//                 app.get("$mainUrl/ajax/embed/episode/$mediaId/sources")
//                         .parsedSafe<VidsrctoEpisodeSources>()
//                         ?: return
//         if (res.status != 200) return
//         res.result?.amap { source ->
//             try {
//                 val embedRes =
//                         app.get("$mainUrl/ajax/embed/source/${source.id}")
//                                 .parsedSafe<VidsrctoEmbedSource>()
//                                 ?: return@amap
//                 val finalUrl = DecryptUrl(embedRes.result.encUrl)
//                 if (finalUrl.equals(embedRes.result.encUrl)) return@amap
//                 val domain = getBaseUrl(finalUrl)
//                 when (source.title) {
//                     "Vidplay" ->
//                             AnyVidplay(finalUrl.substringBefore("/e/"))
//                                     .getUrl(finalUrl, domain, subtitleCallback, callback)
//                     "Filemoon" -> FileMoon().getUrl(finalUrl, domain, subtitleCallback, callback)
//                 }
//             } catch (e: Exception) {
//                 e.message
//             }
//         }
//     }

//     private fun DecryptUrl(encUrl: String): String {
//         var data = encUrl.toByteArray()
//         data = Base64.decode(data, Base64.URL_SAFE)
//         val rc4Key = SecretKeySpec("WXrUARXb1aDLaZjI".toByteArray(), "RC4")
//         val cipher = Cipher.getInstance("RC4")
//         cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
//         data = cipher.doFinal(data)
//         return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
//     }

//     data class VidsrctoEpisodeSources(
//             @JsonProperty("status") val status: Int,
//             @JsonProperty("result") val result: List<VidsrctoResult>?
//     )

//     data class VidsrctoResult(
//             @JsonProperty("id") val id: String,
//             @JsonProperty("title") val title: String
//     )

//     data class VidsrctoEmbedSource(
//             @JsonProperty("status") val status: Int,
//             @JsonProperty("result") val result: VidsrctoUrl
//     )

//     data class VidsrctoUrl(@JsonProperty("url") val encUrl: String)
// }
