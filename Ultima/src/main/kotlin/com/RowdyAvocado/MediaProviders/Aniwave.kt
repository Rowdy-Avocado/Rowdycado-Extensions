package com.RowdyAvocado

import com.RowdyAvocado.UltimaMediaProvidersUtils.ServerName
import com.RowdyAvocado.UltimaMediaProvidersUtils.commonLinkLoader
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AniwaveMediaProvider : MediaProvider() {
    override val name = "Aniwave"
    override val domain = "https://aniwave.best"
    override val categories = listOf(Category.ANIME)
    private val xmlHeader = mapOf("x-requested-with" to "XMLHttpRequest")

    override suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val episode = data.episode ?: if (data.isAnime && data.type.equals("Movie")) 1 else return
        val filterUrl =
                "$url/filter?keyword=${data.title}&year[]=${data.year?:""}&sort=most_relevance"
        val searchPage = app.get(filterUrl).document
        val id =
                searchPage.selectFirst("div.poster")?.attr("data-tip")?.split("?/")?.get(0)
                        ?: return
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf="
        val seasonData =
                app.get(seasonDataUrl, headers = xmlHeader).parsedSafe<ApiResponseHTML>()?.html()
                        ?: return
        val episodeIds =
                seasonData
                        .body()
                        .select(".episodes > ul > li > a")
                        .find { it.attr("data-num").equals(episode.toString()) }
                        ?.attr("data-ids")
                        ?: return
        val episodeDataUrl = "$url/ajax/server/list?servers=$episodeIds"
        val episodeData =
                app.get(episodeDataUrl, headers = xmlHeader).parsedSafe<ApiResponseHTML>()?.html()
                        ?: return

        episodeData.body().select(".servers .type").apmap {
            val dubType = it.attr("data-type")
            it.select("li.ep-server-item").apmap LinkLoader@{
                val serverId = it.attr("data-sv-id")
                val dataId = it.attr("data-link-id")
                val serverResUrl = "$url/ajax/server?get=$dataId"
                val serverRes =
                        app.get(serverResUrl, headers = xmlHeader).parsedSafe<ApiResponseServer>()
                val link = serverRes?.result?.url ?: return@LinkLoader
                commonLinkLoader(
                        name,
                        mapServerName(serverId),
                        link,
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
            "0f2" -> return ServerName.Vidplay // working
            "089" -> return ServerName.MyCloud // working
            // "5c2" -> return ServerName.Vidstreaming // not working
            // "224" -> return ServerName.Gogo // not working
            "5c3" -> return ServerName.StreamWish // working
            "f64" -> return ServerName.Mp4upload // working
            "731" -> return ServerName.DoodStream // working
            "478" -> return ServerName.Vidhide // working
            "c4f" -> return ServerName.Filelions // working
            "323" -> return ServerName.Zoro // working
        }
        return ServerName.NONE
    }

    // #region - Encryption and Decryption handlers

    private fun vrfEncrypt(keys: Keys, input: String): String {
        var vrf = input
        keys.aniwave.sortedBy { it.sequence }.forEach { step ->
            when (step.method) {
                "exchange" -> vrf = exchange(vrf, step.keys?.get(0) ?: "", step.keys?.get(1) ?: "")
                "rc4" -> vrf = rc4Encryption(step.keys?.get(0) ?: "", vrf)
                "reverse" -> vrf = vrf.reversed()
                "base64" -> vrf = base64Encode(vrf.toByteArray())
                "else" -> {}
            }
        }
        vrf = java.net.URLEncoder.encode(vrf, "UTF-8")
        return vrf
    }

    private fun vrfDecrypt(keys: Keys, input: String): String {
        var vrf = input
        keys.aniwave.sortedByDescending { it.sequence }.forEach { step ->
            when (step.method) {
                "exchange" -> vrf = exchange(vrf, step.keys?.get(1) ?: "", step.keys?.get(0) ?: "")
                "rc4" -> vrf = rc4Decryption(step.keys?.get(0) ?: "", vrf)
                "reverse" -> vrf = vrf.reversed()
                "base64" -> vrf = base64Decode(vrf)
                "else" -> {}
            }
        }
        return URLDecoder.decode(vrf, "utf-8")
    }

    private fun rc4Encryption(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)

        var output = cipher.doFinal(input.toByteArray())
        output = base64Encode(output).encodeToByteArray()
        // vrf = base64Encode(vrf, Base64.DEFAULT or Base64.NO_WRAP)
        // vrf = vrfShift(vrf)
        // // vrf = rot13(vrf)
        // vrf = vrf.reversed().toByteArray()
        // vrf = base64Encode(vrf, Base64.URL_SAFE or Base64.NO_WRAP)]
        return output.toString(Charsets.UTF_8)
    }

    private fun rc4Decryption(key: String, input: String): String {
        var vrf = base64DecodeArray(input)

        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return vrf.toString(Charsets.UTF_8)
    }

    fun exchange(input: String, key1: String, key2: String): String {
        return input
                .map { i ->
                    val index = key1.indexOf(i)
                    if (index != -1) {
                        key2[index]
                    } else {
                        i
                    }
                }
                .joinToString("")
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
