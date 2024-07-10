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

class AniwaveMediaProvider : MediaProvider() {
    override val name = "Aniwave"
    override val domain = "https://aniwave.to"
    override val categories = listOf(Category.ANIME)

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val episode = data.episode ?: if (data.isAnime && data.type.equals("Movie")) 1 else return
        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.title}&year[]=${data.year?:""}&sort=most_relevance"
                        )
                        .document
        val id =
                searchPage.selectFirst("div.poster")?.attr("data-tip")?.split("?/")?.get(0)
                        ?: return
        val idVrf = vrfEncrypt(getKeys().aniwave.first(), id)
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=$idVrf"
        val seasonData = app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html() ?: return
        val episodeIds =
                seasonData
                        .body()
                        .select(".episodes > ul > li > a")
                        .find { it.attr("data-num").equals(episode.toString()) }
                        ?.attr("data-ids")
                        ?: return
        val episodeIdsVrf = vrfEncrypt(getKeys().aniwave.first(), episodeIds)
        val episodeDataUrl = "$url/ajax/server/list/$episodeIds?vrf=$episodeIdsVrf"
        val episodeData = app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html() ?: return

        episodeData.body().select(".servers .type").apmap {
            val dubType = it.attr("data-type")
            it.select("li").apmap LinkLoader@{
                val serverId = it.attr("data-sv-id")
                val dataId = it.attr("data-link-id")
                val dataIdVrf = vrfEncrypt(getKeys().aniwave.first(), dataId)
                val serverResUrl = "$url/ajax/server/$dataId?vrf=$dataIdVrf"
                val serverRes = app.get(serverResUrl).parsedSafe<ApiResponseServer>()
                val encUrl = serverRes?.result?.url ?: return@LinkLoader
                val decUrl = vrfDecrypt(getKeys().aniwave.last(), encUrl)
                commonLinkLoader(
                        name,
                        mapServerName(serverId),
                        decUrl,
                        null,
                        dubType,
                        subtitleCallback,
                        callback
                )
            }
        }
    }

    fun mapServerName(id: String): ServerName {
        when (id) {
            "41" -> return ServerName.Vidplay
            "28" -> return ServerName.MyCloud
            "44" -> return ServerName.Filemoon
            "35" -> return ServerName.Mp4upload
        }
        return ServerName.NONE
    }

    // #region - Encryption and Decryption handlers
    fun vrfEncrypt(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var vrf = cipher.doFinal(input.toByteArray())
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        vrf = Base64.encode(vrf, Base64.DEFAULT or Base64.NO_WRAP)
        vrf = vrfShift(vrf)
        // vrf = rot13(vrf)
        vrf = vrf.reversed().toByteArray()
        vrf = Base64.encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)
        val stringVrf = vrf.toString(Charsets.UTF_8)
        val final = java.net.URLEncoder.encode(stringVrf, "utf-8")
        return final
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
            val shift = arrayOf(-2, -4, -5, 6, 2, -3, 3, 6)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
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
