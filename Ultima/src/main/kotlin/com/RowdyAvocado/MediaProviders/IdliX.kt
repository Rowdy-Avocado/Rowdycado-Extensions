package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName
import com.RowdyAvocado.UltimaMediaProvidersUtils.commonLinkLoader
import com.RowdyAvocado.UltimaMediaProvidersUtils.getBaseUrl
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.helper.AesHelper.cryptoAESHandler
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import org.jsoup.Jsoup

@OptIn(kotlin.ExperimentalStdlibApi::class)
class IdliXMediaProvider : MediaProvider() {
    override val name = "IdliX"
    override val domain = "https://tv.idlixofficial.co"
    override val categories = listOf(Category.MEDIA)

    fun createSlug(data: String?): String? {
        return data
                ?.filter { it.isWhitespace() || it.isLetterOrDigit() }
                ?.trim()
                ?.replace("\\s+".toRegex(), "-")
                ?.lowercase()
    }

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = createSlug(data.title)
        val mediaUrl =
                if (data.season == null) {
                    "$url/movie/$fixTitle-${data.year}"
                } else {
                    "$url/episode/$fixTitle-season-${data.season}-episode-${data.episode}"
                }
        wpMoviesExtractor(name, mediaUrl, subtitleCallback, callback, encrypt = true)
    }

    private suspend fun wpMoviesExtractor(
            providerName: String?,
            url: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit,
            fixIframe: Boolean = false,
            encrypt: Boolean = false,
            hasCloudflare: Boolean = false,
            interceptor: Interceptor? = null,
    ) {

        fun String.fixBloat(): String {
            return this.replace("\"", "").replace("\\", "")
        }

        val res = app.get(url ?: return, interceptor = if (hasCloudflare) interceptor else null)
        val referer = getBaseUrl(res.url)
        val document = res.document
        document.select("ul#playeroptionsul > li")
                .map { Triple(it.attr("data-post"), it.attr("data-nume"), it.attr("data-type")) }
                .apmap { (id, nume, type) ->
                    delay(1000)
                    val json =
                            app.post(
                                    url = "$referer/wp-admin/admin-ajax.php",
                                    data =
                                            mapOf(
                                                    "action" to "doo_player_ajax",
                                                    "post" to id,
                                                    "nume" to nume,
                                                    "type" to type
                                            ),
                                    headers =
                                            mapOf(
                                                    "Accept" to "*/*",
                                                    "X-Requested-With" to "XMLHttpRequest"
                                            ),
                                    referer = url,
                                    interceptor = if (hasCloudflare) interceptor else null
                            )
                    val source =
                            tryParseJson<ResponseHash>(json.text)?.let {
                                when {
                                    encrypt -> {
                                        val meta =
                                                tryParseJson<IdliXEmbed>(it.embed_url)?.meta
                                                        ?: return@apmap
                                        val key = generateWpKey(it.key ?: return@apmap, meta)
                                        cryptoAESHandler(it.embed_url, key.toByteArray(), false)
                                                ?.fixBloat()
                                    }
                                    fixIframe ->
                                            Jsoup.parse(it.embed_url).select("IFRAME").attr("SRC")
                                    else -> it.embed_url
                                }
                            }
                                    ?: return@apmap
                    when {
                        !source.contains("youtube") -> {
                            commonLinkLoader(
                                    providerName,
                                    ServerName.Jeniusplay,
                                    source,
                                    null,
                                    null,
                                    subtitleCallback,
                                    callback
                            )
                        }
                    }
                }
    }

    // #region - Encryption and Decryption handlers
    fun generateWpKey(r: String, m: String): String {
        val rList = r.split("\\x").toTypedArray()
        var n = ""
        val decodedM = String(base64Decode(m.split("").reversed().joinToString("")).toCharArray())
        for (s in decodedM.split("|")) {
            n += "\\x" + rList[Integer.parseInt(s) + 1]
        }
        return n
    }
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class ResponseHash(
            @JsonProperty("embed_url") val embed_url: String,
            @JsonProperty("key") val key: String? = null,
            @JsonProperty("type") val type: String? = null,
    )

    data class IdliXEmbed(
            @JsonProperty("m") val meta: String? = null,
    )
    // #endregion - Data classes
}
