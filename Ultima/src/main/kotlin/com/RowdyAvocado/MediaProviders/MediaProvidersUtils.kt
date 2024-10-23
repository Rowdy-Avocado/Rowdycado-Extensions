package com.RowdyAvocado

import com.RowdyAvocado.MoviesDriveProvider.Mdrive
import com.RowdyAvocado.UltimaUtils.Category
import com.RowdyAvocado.UltimaUtils.LinkData
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Jeniusplay
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.extractors.Rabbitstream
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import java.net.URL
import kotlin.io.encoding.Base64

abstract class MediaProvider {
    abstract val name: String
    abstract val domain: String
    abstract val categories: List<Category>

    abstract suspend fun loadContent(
            url: String,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    )

    companion object {
        var keys: Keys? = null
    }

    open suspend fun getKeys(): Keys {
        if (keys == null) {
            keys =
                    app.get("https://rowdy-avocado.github.io/multi-keys/").parsedSafe<Keys>()
                            ?: throw Exception("Unable to fetch keys")
        }
        return keys!!
    }

    data class Keys(
            @JsonProperty("chillx") val chillx: List<String>,
            @JsonProperty("aniwave") val aniwave: List<Step>,
            @JsonProperty("cinezone") val cinezone: List<Step>,
            @JsonProperty("vidplay") val vidplay: List<String>
    ) {
        data class Step(
                @JsonProperty("sequence") val sequence: Int,
                @JsonProperty("method") val method: String,
                @JsonProperty("keys") val keys: List<String>? = null
        )
    }
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
object UltimaMediaProvidersUtils {
    val mediaProviders =
            listOf<MediaProvider>(
                    AniwaveMediaProvider(),
                    CineZoneMediaProvider(),
                    IdliXMediaProvider(),
                    MoflixMediaProvider(),
                    RidomoviesMediaProvider(),
                    VidsrcToMediaProvider(),
                    NowTvMediaProvider(),
                    DahmerMoviesMediaProvider(),
                    NoverseMediaProvider(),
                    AllMovielandMediaProvider(),
                    TwoEmbedMediaProvider(),
                    EMoviesMediaProvider(),
                    MultiEmbededAPIProvider(),
                    MultiMoviesProvider(),
                    AnitakuMediaProvider(),
                    MoviesDriveProvider(),
                    VidSrcMediaProvider(),
                    HiAnimeMediaProvider()
            )

    suspend fun invokeExtractors(
            category: Category,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        UltimaStorageManager.currentMediaProviders.toList().amap {
            val provider = it.getProvider()
            if (provider.categories.contains(category) && it.enabled) {
                try {
                    provider.loadContent(it.getDomain(), data, subtitleCallback, callback)
                } catch (e: Exception) {}
            }
        }
    }

    enum class ServerName {
        MyCloud,
        Mp4upload,
        Streamtape,
        Vidplay,
        Filemoon,
        Jeniusplay,
        Uqload,
        StreamWish,
        Vidhide,
        DoodStream,
        Gogo,
        MDrive,
        Megacloud,
        Filelions,
        Zoro,
        Custom,
        NONE
    }

    fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }

    fun getEpisodeSlug(
            season: Int? = null,
            episode: Int? = null,
    ): Pair<String, String> {
        return if (season == null && episode == null) {
            "" to ""
        } else {
            (if (season!! < 10) "0$season" else "$season") to
                    (if (episode!! < 10) "0$episode" else "$episode")
        }
    }

    fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Qualities.Unknown.value
    }

    fun getIndexQualityTags(str: String?, fullTag: Boolean = false): String {
        return if (fullTag)
                Regex("(?i)(.*)\\.(?:mkv|mp4|avi)").find(str ?: "")?.groupValues?.get(1)?.trim()
                        ?: str ?: ""
        else
                Regex("(?i)\\d{3,4}[pP]\\.?(.*?)\\.(mkv|mp4|avi)")
                        .find(str ?: "")
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.replace(".", " ")
                        ?.trim()
                        ?: str ?: ""
    }

    fun String.encodeUrl(): String {
        val url = URL(this)
        val uri = URI(url.protocol, url.userInfo, url.host, url.port, url.path, url.query, url.ref)
        return uri.toURL().toString()
    }

    fun String?.createSlug(): String? {
        return this?.filter { it.isWhitespace() || it.isLetterOrDigit() }
                ?.trim()
                ?.replace("\\s+".toRegex(), "-")
                ?.lowercase()
    }

    fun fixUrl(url: String, domain: String): String {
        if (url.startsWith("http")) {
            return url
        }
        if (url.isEmpty()) {
            return ""
        }

        val startsWithNoHttp = url.startsWith("//")
        if (startsWithNoHttp) {
            return "https:$url"
        } else {
            if (url.startsWith('/')) {
                return domain + url
            }
            return "$domain/$url"
        }
    }

    // #region - Main Link Handler
    suspend fun commonLinkLoader(
            providerName: String?,
            serverName: ServerName?,
            url: String,
            referer: String?,
            dubStatus: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit,
            quality: Int = Qualities.Unknown.value,
            isM3u8: Boolean = false,
            tag: String? = null
    ) {
        try {
            val domain = referer ?: getBaseUrl(url)
            when (serverName) {
                ServerName.Vidplay ->
                        AnyVidplay(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.MyCloud ->
                        AnyMyCloud(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Filemoon ->
                        AnyFileMoon(providerName, dubStatus, domain)
                                .getUrl(url, null, subtitleCallback, callback)
                ServerName.Mp4upload ->
                        AnyMp4Upload(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Jeniusplay ->
                        AnyJeniusplay(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Uqload ->
                        AnyUqload(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.StreamWish ->
                        AnyStreamwish(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Vidhide ->
                        AnyVidhide(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.DoodStream ->
                        AnyDoodExtractor(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Megacloud ->
                        AnyMegacloud(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Gogo -> {
                    val name =
                            (providerName?.let { "$it: " } ?: "")
                                    .plus("Vidstreaming")
                                    .plus(dubStatus?.let { ": $it" } ?: "")
                    val iv = "3134003223491201"
                    val secretKey = "37911490979715163134003223491201"
                    val secretDecryptKey = "54674138327930866480207815084989"
                    GogoHelper.extractVidstream(
                            url,
                            name,
                            callback,
                            iv,
                            secretKey,
                            secretDecryptKey,
                            isUsingAdaptiveKeys = false,
                            isUsingAdaptiveData = true
                    )
                }
                ServerName.MDrive ->
                        AnyMDrive(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Filelions ->
                        AnyFilelions(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Zoro ->
                        ZoroExtractor(providerName, dubStatus, domain)
                                .getUrl(url, domain, subtitleCallback, callback)
                ServerName.Custom -> {
                    callback.invoke(
                            ExtractorLink(
                                    providerName ?: return,
                                    tag?.let { "$providerName: $it" } ?: providerName,
                                    url,
                                    domain,
                                    quality,
                                    isM3u8
                            )
                    )
                }
                else -> {
                    loadExtractor(url, subtitleCallback, callback)
                }
            }
        } catch (e: Exception) {}
    }
    // #endregion - Main Link Handler
}

// #region - Custom Extractors
class AnyFileMoon(provider: String?, dubType: String?, domain: String = "") : Filesim() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "Filemoon" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
class AnyMyCloud(provider: String?, dubType: String?, domain: String = "") : Vidplay() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "MyCloud" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val encIFrameUrl = app.get(url).url.split("#").getOrNull(1) ?: return
        val fileLink = Base64.UrlSafe.decode(encIFrameUrl).toString(Charsets.UTF_8)
        callback.invoke(
                ExtractorLink(
                        name,
                        name,
                        fileLink,
                        "",
                        Qualities.Unknown.value,
                        fileLink.contains(".m3u8")
                )
        )
    }
}

class AnyVidplay(provider: String?, dubType: String?, domain: String = "") : Vidplay() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "Vidplay" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val iFramePage = app.get(url, referer = referer).document
        val jsData = iFramePage.selectFirst("script:containsData(jwplayer)") ?: return
        val fileLink = Regex("""file": `(.*)`""").find(jsData.html())?.groupValues?.get(1) ?: return
        callback.invoke(
                ExtractorLink(
                        name,
                        name,
                        fileLink,
                        "",
                        Qualities.Unknown.value,
                        fileLink.contains(".m3u8")
                )
        )
    }
}

class AnyMp4Upload(provider: String?, dubType: String?, domain: String = "") : Mp4Upload() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Mp4Upload" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyJeniusplay(provider: String?, dubType: String?, domain: String = "") : Jeniusplay() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "JeniusPlay" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyUqload(provider: String?, dubType: String?, domain: String = "") : Filesim() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "Uqloads" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false
}

class AnyStreamwish(provider: String?, dubType: String?, domain: String = "") :
        StreamWishExtractor() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "SteamWish" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyVidhide(provider: String?, dubType: String?, domain: String = "") : VidhideExtractor() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Vidhide" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyDoodExtractor(provider: String?, dubType: String?, domain: String = "") :
        DoodLaExtractor() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "DoodStream" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyMDrive(provider: String?, dubType: String?, domain: String = "") : Mdrive() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "MDrive" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class AnyMegacloud(provider: String?, dubType: String?, domain: String = "") : Rabbitstream() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Megacloud" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain

    override val embed = "embed-2/ajax/e-1"
    private val scriptUrl = "$mainUrl/js/player/a/prod/e1-player.min.js"

    override suspend fun extractRealKey(sources: String): Pair<String, String> {
        val rawKeys = getKeys()
        val sourcesArray = sources.toCharArray()

        var extractedKey = ""
        var currentIndex = 0
        for (index in rawKeys) {
            val start = index[0] + currentIndex
            val end = start + index[1]
            for (i in start until end) {
                extractedKey += sourcesArray[i].toString()
                sourcesArray[i] = ' '
            }
            currentIndex += index[1]
        }

        return extractedKey to sourcesArray.joinToString("").replace(" ", "")
    }

    private suspend fun getKeys(): List<List<Int>> {
        val script = app.get(scriptUrl).text
        fun matchingKey(value: String): String {
            return Regex(",$value=((?:0x)?([0-9a-fA-F]+))")
                    .find(script)
                    ?.groupValues
                    ?.get(1)
                    ?.removePrefix("0x")
                    ?: throw ErrorLoadingException("Failed to match the key")
        }

        val regex =
                Regex(
                        "case\\s*0x[0-9a-f]+:(?![^;]*=partKey)\\s*\\w+\\s*=\\s*(\\w+)\\s*,\\s*\\w+\\s*=\\s*(\\w+);"
                )
        val indexPairs =
                regex.findAll(script)
                        .toList()
                        .map { match ->
                            val matchKey1 = matchingKey(match.groupValues[1])
                            val matchKey2 = matchingKey(match.groupValues[2])
                            try {
                                listOf(matchKey1.toInt(16), matchKey2.toInt(16))
                            } catch (e: NumberFormatException) {
                                emptyList()
                            }
                        }
                        .filter { it.isNotEmpty() }

        return indexPairs
    }
}

class AnyFilelions(provider: String?, dubType: String?, domain: String = "") : VidhideExtractor() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Filelions" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class ZoroExtractor(provider: String?, dubType: String?, domain: String = "") : ExtractorApi() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Zoro" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false

    data class ZoroJson(
            @JsonProperty("tracks") val subtitles: List<Subtitle>,
            @JsonProperty("sources") val sources: List<Source>
    ) {
        data class Subtitle(
                @JsonProperty("file") val file: String,
                @JsonProperty("label") val lang: String? = null,
                @JsonProperty("kind") val kind: String,
        )

        data class Source(
                @JsonProperty("file") val file: String,
                @JsonProperty("type") val type: String
        )
    }

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val iFramePage = app.get(url, referer = referer).document
        val jsData =
                iFramePage
                        .selectFirst("script:containsData(JsonData)")
                        ?.html()
                        ?.split("=")
                        ?.getOrNull(1)
                        ?: return
        val jsonData = AppUtils.parseJson<ZoroJson>(jsData)
        jsonData.subtitles.amap { sub ->
            sub.lang?.let { subtitleCallback.invoke(SubtitleFile(it, sub.file)) }
        }
        jsonData.sources.amap { source ->
            callback.invoke(
                    ExtractorLink(
                            name,
                            name,
                            source.file,
                            "",
                            Qualities.Unknown.value,
                            source.file.contains(".m3u8")
                    )
            )
        }
    }
}
// #endregion - Custom Extractors
