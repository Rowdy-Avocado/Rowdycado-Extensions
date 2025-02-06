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
import com.lagradost.cloudstream3.utils.StringUtils.decodeUri
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

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
        val idVrf = vrfEncrypt(getKeys(), id)
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
        val epVrf = vrfEncrypt(getKeys(), episodeId)
        val episodeDataUrl = "$url/ajax/server/list/$episodeId?vrf=$epVrf"
        val episodeData = app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html() ?: return

        episodeData.body().select(".server").apmap {
            val serverId = it.attr("data-id")
            val dataId = it.attr("data-link-id")
            val dataVrf = vrfEncrypt(getKeys(), dataId)
            val serverResUrl = "$url/ajax/server/$dataId?vrf=$dataVrf"
            val serverRes = app.get(serverResUrl).parsedSafe<ApiResponseServer>()
            val encUrl = serverRes?.result?.url ?: return@apmap
            val decUrl = vrfDecrypt(getKeys(), encUrl)
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
    fun vrfEncrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.cinezone.sortedBy { it.sequence }.forEach { step ->
			when(step.method) {
				"exchange" -> vrf = exchange(vrf, step.keys?.get(0) ?: "", step.keys?.get(1) ?: "")
				"rc4" -> vrf = rc4Encryption(step.keys?.get(0) ?: "", vrf)
				"reverse" -> vrf = vrf.reversed()
				"base64" -> vrf = base64Encode(vrf.toByteArray())
				"else" -> {}
			}
		}
		vrf = vrf.encodeUri()
		return vrf
    }

    fun vrfDecrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.cinezone.sortedByDescending { it.sequence }.forEach { step ->
			when(step.method) {
				"exchange" -> vrf = exchange(vrf, step.keys?.get(1) ?: "", step.keys?.get(0) ?: "")
				"rc4" -> vrf = rc4Decryption(step.keys?.get(0) ?: "", vrf)
				"reverse" -> vrf = vrf.reversed()
				"base64" -> vrf = base64Decode(vrf)
				"else" -> {}
			}
		}
		return vrf.decodeUri()
	}

	private fun rc4Encryption(key: String, input: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        var output = cipher.doFinal(input.toByteArray())
        return base64Encode(output)
	}

    private fun rc4Decryption(key: String, input: String): String {
        var vrf = base64DecodeArray(input)
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        vrf = cipher.doFinal(vrf)

        return vrf.toString(Charsets.UTF_8)
    }
	
	private fun exchange(input: String, key1: String, key2: String): String {
		return input.map { i -> 
			val index = key1.indexOf(i)
			if (index != -1) {
				key2[index]
			} else {
				i
			}
		}.joinToString("")
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
