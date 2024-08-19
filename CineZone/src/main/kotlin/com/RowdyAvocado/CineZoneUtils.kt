package com.RowdyAvocado

import android.util.Base64
import java.net.URI
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.RowdyAvocado.CineZone.Keys

@OptIn(kotlin.ExperimentalStdlibApi::class)
object CineZoneUtils {

    fun vrfEncrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.cinezone.sortedBy { it.sequence }.forEach { step ->
			when(step.method) {
				"exchange" -> vrf = exchange(vrf, step.keys?.get(0) ?: "", step.keys?.get(1) ?: "")
				"rc4" -> vrf = rc4Encryption(step.keys?.get(0) ?: "", vrf)
				"reverse" -> vrf = vrf.reversed()
				"base64" -> vrf = Base64.encode(vrf.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP).toString(Charsets.UTF_8)
				"else" -> {}
			}
		}
		vrf = java.net.URLEncoder.encode(vrf, "UTF-8")
		return vrf
    }

    fun vrfDecrypt(keys: Keys, input: String): String {
		var vrf = input
		keys.cinezone.sortedByDescending { it.sequence }.forEach { step ->
			when(step.method) {
				"exchange" -> vrf = exchange(vrf, step.keys?.get(1) ?: "", step.keys?.get(0) ?: "")
				"rc4" -> vrf = rc4Decryption(step.keys?.get(0) ?: "", vrf)
				"reverse" -> vrf = vrf.reversed()
				"base64" -> vrf = Base64.decode(vrf, Base64.URL_SAFE).toString(Charsets.UTF_8)
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
        output = Base64.encode(output, Base64.URL_SAFE or Base64.NO_WRAP)
        return output.toString(Charsets.UTF_8)
	}

    private fun rc4Decryption(key: String, input: String): String {
        var vrf = input.toByteArray()
        vrf = Base64.decode(vrf, Base64.URL_SAFE)
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
            val shift = arrayOf(4, 3, -2, 5, 2, -4, -4, 2)[i % 8]
            vrf[i] = vrf[i].plus(shift).toByte()
        }
        return vrf
    }

    fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }
}
