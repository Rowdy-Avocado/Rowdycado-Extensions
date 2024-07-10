package com.RowdyAvocado

import android.graphics.BitmapFactory
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.extractors.Rabbitstream
import java.net.URL
import kotlin.collections.toByteArray
import kotlin.emptyArray
import kotlin.math.roundToInt

class Megacloud2 : Rabbitstream() {
    override val name = "Megacloud2"
    override val mainUrl = "https://megacloud.tv"
    override val embed = "embed-1/ajax/e-1"
    private val scriptUrl = "$mainUrl/js/player/a/prod/e1-player.min.js"
    private val luckyImageUrl = "$mainUrl/images/lucky_animal/icon.png"

    // similar code source for future reference available at
    // https://github.com/rhenwinch/Flixclusive/master/extractor/upcloud/src/main/kotlin/com/flixclusive/extractor/upcloud/VidCloud.kt

    override suspend fun extractRealKey(sources: String): Pair<String, String> {
        val imageMap = BitmapFactory.decodeStream(URL(luckyImageUrl).openStream())

        val width = imageMap.width
        val height = imageMap.height
        var pixelData = emptyArray<Int>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = imageMap.getColor(x, y)
                pixelData += pixel.red().toInt()
                pixelData += pixel.green().toInt()
                pixelData += pixel.blue().toInt()
                pixelData += (pixel.alpha() * 255).roundToInt()
            }
        }

        val encodedByteArray = computeKeyFromImage(pixelData)

        val key = base64Encode(encodedByteArray)
        return key to sources
    }

    private fun computeKeyFromImage(image: Array<Int>): ByteArray {
        var imageChunks = ""
        var imageChunksToChar = ""
        var imageChunksToCharToHex = emptyArray<Int>()

        for (i in 0 until (image[3] * 8)) {
            imageChunks += image[(i + 1) * 4 + 3] % 2
        }

        imageChunks.dropLast(imageChunks.length % 2).chunked(8).forEach { chunks ->
            imageChunksToChar += chunks.toInt(2).toChar()
        }

        for (i in 0 until (imageChunksToChar.length - 1) step 2) {
            imageChunksToCharToHex += imageChunksToChar.substring(i, i + 2).toInt(16)
        }

        var key = imageChunksToCharToHex.map { it.toByte() }.toList().toByteArray()
        return key
    }
}
