package com.RowdyAvocado

import com.RowdyAvocado.Aniwave.Keys
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import java.net.URI
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalStdlibApi::class)
object AniwaveUtils {

    fun vrfEncrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.aniwave.sortedBy { it.sequence }.forEach { step ->
			when(step.method) {
				"exchange" -> vrf = exchange(vrf, step.keys?.get(0) ?: "", step.keys?.get(1) ?: "")
				"rc4" -> vrf = rc4Encryption(step.keys?.get(0) ?: "", vrf)
				"reverse" -> vrf = vrf.reversed()
				"base64" -> vrf = base64Encode(vrf.toByteArray())
				"else" -> {}
			}
		}
		vrf = java.net.URLEncoder.encode(vrf, "UTF-8")
		return "vrf=$vrf"
    }

    fun vrfDecrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.aniwave.sortedByDescending { it.sequence }.forEach { step ->
			when(step.method) {
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
        // vrf = base64Encode(vrf)
        // vrf = vrfShift(vrf)
        // // vrf = rot13(vrf)
        // vrf = vrf.reversed().toByteArray()
        // vrf = base64Encode(vrf)
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

    fun aniQuery(name: SyncIdName, id: Int): String {
        // creating query for Anilist API using ID
        val idType =
                when (name) {
                    SyncIdName.MyAnimeList -> "idMal"
                    else -> "id"
                }
        val query =
                """
            query {
                Media($idType: $id, type: ANIME) {
                    title {
                        romaji
                        english
                    }
                    id
                    idMal
                    season
                    seasonYear
                }
            }
        """
        return query
    }

    fun aniQuery(titleRomaji: String, year: Int, season: String): String {
        // creating query for Anilist API using name and other details
        val query =
                """
            query {
                Media(search: "$titleRomaji", season:${season.uppercase()}, seasonYear:$year, type: ANIME) {
                    title {
                        romaji
                        english
                    }
                    id
                    idMal
                    season
                    seasonYear
                }
            }
        """
        return query
    }

    fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }
}
