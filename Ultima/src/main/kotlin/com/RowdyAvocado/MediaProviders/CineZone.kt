package com.RowdyAvocado

import android.util.Base64
import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName
import com.RowdyAvocado.UltimaMediaProvidersUtils.commonLinkLoader
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CineZoneMediaProvider : MediaProvider() {
    override val name = "CineZone"
    override val domain = "https://cinezone.to"
    override val categories = listOf(Category.MEDIA)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {

        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.title}&year[]=${data.year?:""}&sort=most_relevance"
                        )
                        .document
        val id =
                searchPage.selectFirst("div.tooltipBtn")?.attr("data-tip")?.split("?/")?.get(0)
                        ?: return
        val idVrf = vrfEncrypt(getKeys().cinezone.first(), id)
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=$idVrf"
        val seasonData = app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html() ?: return
        val episodeId =
                seasonData
                        .body()
                        .select(".episodes")
                        .find { it.attr("data-season").equals(data.season?.toString() ?: "1") }
                        ?.select("li a")
                        ?.find { it.attr("data-num").equals(data.episode?.toString() ?: "1") }
                        ?.attr("data-id")
                        ?: return
        val epVrf = vrfEncrypt(getKeys().cinezone.first(), episodeId)
        val episodeDataUrl = "$url/ajax/server/list/$episodeId?vrf=$epVrf"
        val episodeData = app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html() ?: return

        episodeData.body().select(".server").apmap {
            val serverId = it.attr("data-id")
            val dataId = it.attr("data-link-id")
            val dataVrf = vrfEncrypt(getKeys().cinezone.first(), dataId)
            val serverResUrl = "$url/ajax/server/$dataId?vrf=$dataVrf"
            val serverRes = app.get(serverResUrl).parsedSafe<ApiResponseServer>()
            val encUrl = serverRes?.result?.url ?: return@apmap
            val decUrl = vrfDecrypt(getKeys().cinezone.last(), encUrl)
            commonLinkLoader(
                    name,
                    mapServerName(serverId),
                    decUrl,
                    null,
                    null,
                    subtitleCallback,
                    callback
            )
        }
    }

    // #region - Encryption and Decryption handlers
    fun vrfEncrypt(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var vrf = cipher.doFinal(input.toByteArray())
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = vrf.reversed().toByteArray()
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = vrfShift(vrf)
        val stringVrf = vrf.toString(Charsets.UTF_8)
        return stringVrf
    }

    fun vrfDecrypt(key: String, input: String): String {
        var vrf = input.toByteArray()
        vrf = Base64.decode(vrf, Base64.URL_SAFE)

        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return URLDecoder.decode(vrf.toString(Charsets.UTF_8), "utf-8")
    }

    @kotlin.ExperimentalStdlibApi
    private fun rot13(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val byte = vrf[i]
            if (byte in 'A'.code..'Z'.code) {
                vrf[i] = ((byte - 'A'.code + 13) % 26 + 'A'.code).toByte()
            } else if (byte in 'a'.code..'z'.code) {
                vrf[i] = ((byte - 'a'.code + 13) % 26 + 'a'.code).toByte()
            }
        }
        return vrf
    }

    private fun vrfShift(vrf: ByteArray): ByteArray {
        for (i in vrf.indices) {
            val shift = arrayOf(4, 3, -2, 5, 2, -4, -4, 2)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
    }

    fun mapServerName(id: String): ServerName {
        when (id) {
            "28" -> return ServerName.MyCloud
            "35" -> return ServerName.Mp4upload
            "40" -> return ServerName.Streamtape
            "41" -> return ServerName.Vidplay
            "45" -> return ServerName.Filemoon
        }
        return ServerName.NONE
    }
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
    data class ApiResponseHTML(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: String
    ) {
        fun html(): Document {
            return Jsoup.parse(result)
        }
    }

    data class ApiResponseServer(
            @JsonProperty("status") val status: Int? = null,
            @JsonProperty("result") val result: Url? = null
    ) {
        data class Url(@JsonProperty("url") val url: String? = null)
    }
    // #endregion - Data classes
}
