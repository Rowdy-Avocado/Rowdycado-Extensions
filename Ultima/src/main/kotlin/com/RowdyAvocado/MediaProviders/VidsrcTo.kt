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
        AnyVidSrcTo(url).getUrl(iFrameUrl, url, subtitleCallback, callback)
    }

    // #region - Encryption and Decryption handlers
    // #endregion - Encryption and Decryption handlers

    // #region - Data classes
	// #endregion - Data classes

	class AnyVidSrcTo(domain: String) : VidSrcTo() {
		override val mainUrl = domain
	}
}

// @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
// open class VidSrcTo : ExtractorApi() {
//     override val name = "VidSrcTo"
//     override val mainUrl = "https://vidsrc.to"
//     override val requiresReferer = true

// 	companion object {
// 		private val keySource = "https://rowdy-avocado.github.io/multi-keys/"

// 		private var keys: KeysData? = null

// 		private suspend fun getKeys(): KeysData {
// 			return keys
// 					?: run {
// 						keys = app.get(keySource).parsedSafe<KeysData>()
// 										?: throw ErrorLoadingException("Unable to get keys")
// 						keys!!
// 					}
// 		}
// 	}

//     override suspend fun getUrl(
//             url: String,
//             referer: String?,
//             subtitleCallback: (SubtitleFile) -> Unit,
//             callback: (ExtractorLink) -> Unit
//     ) {
//         val mediaId = app.get(url).document.selectFirst("ul.episodes li a")?.attr("data-id") ?: return
//         val subtitlesLink = "$mainUrl/ajax/embed/episode/$mediaId/subtitles"
//         val subRes = app.get(subtitlesLink).parsedSafe<Array<VidsrctoSubtitles>>()
//         subRes?.forEach {
//             if (it.kind.equals("captions")) subtitleCallback.invoke(SubtitleFile(it.label, it.file))
//         }
//         val sourcesLink = "$mainUrl/ajax/embed/episode/$mediaId/sources?token=${vrfEncrypt(getKeys(), mediaId)}"
//         val res = app.get(sourcesLink).parsedSafe<VidsrctoEpisodeSources>() ?: return
//         if (res.status != 200) return
//         res.result?.amap { source ->
//             try {
// 				val embedResUrl = "$mainUrl/ajax/embed/source/${source.id}?token=${vrfEncrypt(getKeys(), source.id)}"
//                 val embedRes = app.get(embedResUrl).parsedSafe<VidsrctoEmbedSource>() ?: return@amap
//                 val finalUrl = vrfDecrypt(getKeys(), embedRes.result.encUrl)
//                 if(finalUrl.equals(embedRes.result.encUrl)) return@amap
//                 when (source.title) {
//                     "Server 1" -> AnyVidplay(finalUrl.substringBefore("/e/")).getUrl(finalUrl, referer, subtitleCallback, callback)
//                     "Server 2" -> FileMoon().getUrl(finalUrl, referer, subtitleCallback, callback)
//                 }
//             } catch (e: Exception) {
//                 logError(e)
//             }
//         }
//     }

// 	private fun vrfEncrypt(keys: KeysData, input: String): String {
// 		var vrf = input
// 		keys.vidsrcto.sortedBy { it.sequence }.forEach { step ->
// 			when(step.method) {
// 				"exchange" -> vrf = exchange(vrf, step.keys?.get(0) ?: return@forEach, step.keys?.get(1) ?: return@forEach)
// 				"rc4" -> vrf = rc4Encryption(step.keys?.get(0) ?: return@forEach, vrf)
// 				"reverse" -> vrf = vrf.reversed()
// 				"base64" -> vrf = Base64.UrlSafe.encode(vrf.toByteArray())
// 				"else" -> {}
// 			}
// 		}
// 		// vrf = java.net.URLEncoder.encode(vrf, "UTF-8")
// 		return vrf
// 	}

// 	private fun vrfDecrypt(keys: KeysData, input: String): String {
// 		var vrf = input
// 		keys.vidsrcto.sortedByDescending { it.sequence }.forEach { step ->
// 			when(step.method) {
// 				"exchange" -> vrf = exchange(vrf, step.keys?.get(1) ?: return@forEach, step.keys?.get(0) ?: return@forEach)
// 				"rc4" -> vrf = rc4Decryption(step.keys?.get(0) ?: return@forEach, vrf)
// 				"reverse" -> vrf = vrf.reversed()
// 				"base64" -> vrf = Base64.UrlSafe.decode(vrf).toString(Charsets.UTF_8)
// 				"else" -> {}
// 			}
// 		}
// 		return URLDecoder.decode(vrf, "utf-8")
// 	}

// 	private fun rc4Encryption(key: String, input: String): String {
// 		val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
// 		val cipher = Cipher.getInstance("RC4")
// 		cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
// 		var output = cipher.doFinal(input.toByteArray())
// 		output = Base64.UrlSafe.encode(output).toByteArray()
// 		return output.toString(Charsets.UTF_8)
// 	}

// 	private fun rc4Decryption(key: String, input: String): String {
// 		var vrf = input.toByteArray()
// 		vrf = Base64.UrlSafe.decode(vrf)
// 		val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
// 		val cipher = Cipher.getInstance("RC4")
// 		cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
// 		vrf = cipher.doFinal(vrf)

// 		return vrf.toString(Charsets.UTF_8)
// 	}
	
// 	private fun exchange(input: String, key1: String, key2: String): String {
// 		return input.map { i -> 
// 			val index = key1.indexOf(i)
// 			if (index != -1) {
// 				key2[index]
// 			} else {
// 				i
// 			}
// 		}.joinToString("")
// 	}

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

//     data class VidsrctoSubtitles(
//             @JsonProperty("file") val file: String,
//             @JsonProperty("label") val label: String,
//             @JsonProperty("kind") val kind: String
//     )

//     data class VidsrctoUrl(@JsonProperty("url") val encUrl: String)

//     data class KeysData(@JsonProperty("vidsrcto") val vidsrcto: List<Step>)

// 	data class Step(
// 			@JsonProperty("sequence") val sequence: Int,
// 			@JsonProperty("method") val method: String,
// 			@JsonProperty("keys") val keys: List<String>? = null 
// 	)
// }