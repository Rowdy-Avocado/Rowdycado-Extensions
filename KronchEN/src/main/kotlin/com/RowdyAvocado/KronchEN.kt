package com.RowdyAvocado

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.RowdyAvocado.KronchENPlugin.Companion.postFunction
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AcraApplication.Companion.context
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDate
import com.lagradost.cloudstream3.addDubStatus
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.nicehttp.requestCreator
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

class KronchEN : MainAPI() {

    companion object {
        // var latestHeader: Map<String, String> = emptyMap()
        var latestKrunchyHeader: Map<String, String> = emptyMap()
        // var latestKrunchySession: Map<String, String> = emptyMap()
        var latestcountryID = ""
        private const val krunchyapi = "https://beta-api.crunchyroll.com"
        // private const val kronchyConsumetapi = "https://api.consumet.org/anime/crunchyroll"
    }

    override var name = "Kwunchwoll"
    override var mainUrl = "https://www.crunchyroll.com"
    override val instantLinkLoading = false
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = true
    override val supportedTypes =
            setOf(
                    TvType.AnimeMovie,
                    TvType.Anime,
            )
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    data class KronchyToken(
            @JsonProperty("access_token") val accessToken: String? = null,
            @JsonProperty("expires_in") val expiresIn: Int? = null,
            @JsonProperty("token_type") val tokenType: String? = null,
            @JsonProperty("scope") val scope: String? = null,
            @JsonProperty("country") val country: String? = null
    )

    data class TokensJson(
            @JsonProperty("Rtoken") val Rtoken: String,
            @JsonProperty("Btoken") val Btoken: String
    )

    private suspend fun proxyToken(): Map<String, String> {
        val refreshtoken =
                app.get(
                                "https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/refreshtoken.json"
                        )
                        .parsed<TokensJson>()
        // Thanks to
        // https://github.com/jmir1/aniyomi-extensions/blob/master/src/all/kamyroll/src/eu/kanade/tachiyomi/animeextension/all/kamyroll/AccessTokenInterceptor.kt
        val client = app.baseClient.newBuilder().build()
        val proxy =
                client.newBuilder()
                        .proxy(
                                Proxy(
                                        Proxy.Type.SOCKS,
                                        InetSocketAddress("cr-unblocker.us.to", 1080)
                                )
                        )
                        .build()
        Authenticator.setDefault(
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication("crunblocker", "crunblocker".toCharArray())
                    }
                },
        )

        val response =
                proxy.newCall(
                        requestCreator(
                                "POST",
                                "$krunchyapi/auth/v1/token",
                                headers =
                                        mapOf(
                                                "User-Agent" to
                                                        "Crunchyroll/3.26.1 Android/11 okhttp/4.9.2",
                                                "Content-Type" to
                                                        "application/x-www-form-urlencoded",
                                                "Authorization" to "Basic ${refreshtoken.Btoken}"
                                        ),
                                data =
                                        mapOf(
                                                "refresh_token" to refreshtoken.Rtoken,
                                                "grant_type" to "refresh_token",
                                                "scope" to "offline_access",
                                        )
                        )
                )
        val jsonResponse = parseJson<KronchyToken>(response.execute().body.string())

        val header =
                mapOf("Authorization" to "${jsonResponse.tokenType} ${jsonResponse.accessToken}")
        val countryID = jsonResponse.country ?: "No country"

        latestKrunchyHeader = header
        latestcountryID = countryID
        return latestKrunchyHeader
    }
    /* private suspend fun  proxyToken(): Map<String, String> {
        val refreshtoken = app.get("https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/refreshtoken.json").parsed<TokensJson>()

        val testingasa = app.post("$krunchyapi/auth/v1/token",
            headers = mapOf(
                "User-Agent"  to "Crunchyroll/3.26.1 Android/11 okhttp/4.9.2",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic ${refreshtoken.Btoken}"
            ),
            data = mapOf(
                "refresh_token" to refreshtoken.Rtoken,
                "grant_type" to "refresh_token",
                "scope" to "offline_access",
            )
        ).parsed<KronchyToken>()
        val header = mapOf(
            "Authorization" to "${testingasa.tokenType} ${testingasa.accessToken}"
        )
        val countryID = testingasa.country ?: "No country"
        latestKrunchyHeader = header
        latestcountryID = countryID
        return latestKrunchyHeader
    }

     private suspend fun getConsuToken():Map<String, String> {
         val consuToken = app.get("https://cronchy.consumet.stream/token").parsed<ConsuToken>()
         val header = mapOf(
             "Authorization" to "${consuToken.tokenType} ${consuToken.accessToken}"
         )
         latestKrunchyHeader = header
         latestcountryID = consuToken.country!!
         return latestKrunchyHeader
     } */

    data class PosterTall(
            @JsonProperty("height") var height: Int? = null,
            @JsonProperty("source") var source: String? = null,
            @JsonProperty("type") var type: String? = null,
            @JsonProperty("width") var width: Int? = null
    )

    data class KrunchyHome(
            @JsonProperty("total") val total: Int? = null,
            @JsonProperty("items") val items: ArrayList<KrunchyItems> = arrayListOf(),
            @JsonProperty("__class__") val _class_: String? = null,
            @JsonProperty("__href__") val _href_: String? = null,
            @JsonProperty("__resource_key__") val _resourceKey_: String? = null,
    )

    data class KrunchyItems(
            @JsonProperty("__class__") var _class_: String? = null,
            @JsonProperty("new_content") var newContent: Boolean? = null,
            @JsonProperty("description") var description: String? = null,
            @JsonProperty("__href__") var _href_: String? = null,
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("promo_description") var promoDescription: String? = null,
            @JsonProperty("slug") var slug: String? = null,
            @JsonProperty("channel_id") var channelId: String? = null,
            @JsonProperty("images") var images: KrunchyImages? = KrunchyImages(),
            @JsonProperty("linked_resource_key") var linkedResourceKey: String? = null,
            @JsonProperty("last_public") var lastPublic: String? = null,
            @JsonProperty("slug_title") var slugTitle: String? = null,
            @JsonProperty("external_id") var externalId: String? = null,
            @JsonProperty("series_metadata")
            var seriesMetadata: KrunchySeriesMetadata? = KrunchySeriesMetadata(),
            @JsonProperty("episode_metadata")
            var episodeMetadata: KrunchyEpisodesMetadata? = KrunchyEpisodesMetadata(),
            @JsonProperty("type") var type: String? = null,
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("promo_title") var promoTitle: String? = null,
            @JsonProperty("new") var new: Boolean? = null
    )

    data class KrunchyImages(
            @JsonProperty("poster_tall")
            var posterTall: ArrayList<ArrayList<PosterTall>> = arrayListOf(),
            @JsonProperty("poster_wide")
            var posterWide: ArrayList<ArrayList<PosterTall>> = arrayListOf(),
            @JsonProperty("thumbnail")
            var thumbnail: ArrayList<ArrayList<PosterTall>> = arrayListOf(),
    )
    data class KrunchySeriesMetadata(
            @JsonProperty("audio_locales") var audioLocales: ArrayList<String> = arrayListOf(),
            @JsonProperty("availability_notes") var availabilityNotes: String? = null,
            @JsonProperty("episode_count") var episodeCount: Int? = null,
            @JsonProperty("extended_description") var extendedDescription: String? = null,
            @JsonProperty("is_dubbed") var isDubbed: Boolean? = null,
            @JsonProperty("is_mature") var isMature: Boolean? = null,
            @JsonProperty("is_simulcast") var isSimulcast: Boolean? = null,
            @JsonProperty("is_subbed") var isSubbed: Boolean? = null,
            @JsonProperty("mature_blocked") var matureBlocked: Boolean? = null,
            @JsonProperty("maturity_ratings")
            var maturityRatings: ArrayList<String> = arrayListOf(),
            @JsonProperty("season_count") var seasonCount: Int? = null,
            @JsonProperty("series_launch_year") var seriesLaunchYear: Int? = null,
            @JsonProperty("subtitle_locales") var subtitleLocales: ArrayList<String> = arrayListOf()
    )

    data class KrunchyEpisodesMetadata(
            @JsonProperty("audio_locale") var audioLocale: String? = null,
            @JsonProperty("availability_ends") var availabilityEnds: String? = null,
            @JsonProperty("availability_notes") var availabilityNotes: String? = null,
            @JsonProperty("availability_starts") var availabilityStarts: String? = null,
            @JsonProperty("available_date") var availableDate: String? = null,
            @JsonProperty("available_offline") var availableOffline: Boolean? = null,
            @JsonProperty("closed_captions_available") var closedCaptionsAvailable: Boolean? = null,
            @JsonProperty("duration_ms") var durationMs: Int? = null,
            @JsonProperty("eligible_region") var eligibleRegion: String? = null,
            @JsonProperty("episode") var episode: String? = null,
            @JsonProperty("episode_air_date") var episodeAirDate: String? = null,
            @JsonProperty("episode_number") var episodeNumber: String? = null,
            @JsonProperty("free_available_date") var freeAvailableDate: String? = null,
            @JsonProperty("identifier") var identifier: String? = null,
            @JsonProperty("is_clip") var isClip: Boolean? = null,
            @JsonProperty("is_dubbed") var isDubbed: Boolean? = null,
            @JsonProperty("is_mature") var isMature: Boolean? = null,
            @JsonProperty("is_premium_only") var isPremiumOnly: Boolean? = null,
            @JsonProperty("is_subbed") var isSubbed: Boolean? = null,
            @JsonProperty("mature_blocked") var matureBlocked: Boolean? = null,
            @JsonProperty("maturity_ratings")
            var maturityRatings: ArrayList<String>? = arrayListOf(),
            @JsonProperty("premium_available_date") var premiumAvailableDate: String? = null,
            @JsonProperty("premium_date") var premiumDate: String? = null,
            @JsonProperty("season_id") var seasonId: String? = null,
            @JsonProperty("season_number") var seasonNumber: Int? = null,
            @JsonProperty("season_slug_title") var seasonSlugTitle: String? = null,
            @JsonProperty("season_title") var seasonTitle: String? = null,
            @JsonProperty("sequence_number") var sequenceNumber: Int? = null,
            @JsonProperty("series_id") var seriesId: String? = null,
            @JsonProperty("series_slug_title") var seriesSlugTitle: String? = null,
            @JsonProperty("series_title") var seriesTitle: String? = null,
            @JsonProperty("subtitle_locales")
            var subtitleLocales: ArrayList<String>? = arrayListOf(),
            @JsonProperty("upload_date") var uploadDate: String? = null,
    )

    data class KrunchyLoadMain(
            @JsonProperty("total") var total: Int? = null,
            @JsonProperty("data") var data: ArrayList<KrunchyMetadata> = arrayListOf(),
    )

    data class KrunchyMetadata(
            @JsonProperty("series_launch_year") var seriesLaunchYear: Int? = null,
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("keywords") var keywords: ArrayList<String> = arrayListOf(),
            @JsonProperty("content_provider") var contentProvider: String? = null,
            @JsonProperty("subtitle_locales")
            var subtitleLocales: ArrayList<String> = arrayListOf(),
            @JsonProperty("is_dubbed") var isDubbed: Boolean? = null,
            @JsonProperty("audio_locales") var audioLocales: ArrayList<String> = arrayListOf(),
            @JsonProperty("season_tags") var seasonTags: ArrayList<String> = arrayListOf(),
            @JsonProperty("episode_count") var episodeCount: Int? = null,
            @JsonProperty("season_count") var seasonCount: Int? = null,
            @JsonProperty("is_subbed") var isSubbed: Boolean? = null,
            @JsonProperty("channel_id") var channelId: String? = null,
            @JsonProperty("extended_description") var extendedDescription: String? = null,
            @JsonProperty("seo_description") var seoDescription: String? = null,
            @JsonProperty("is_simulcast") var isSimulcast: Boolean? = null,
            @JsonProperty("availability_notes") var availabilityNotes: String? = null,
            @JsonProperty("slug") var slug: String? = null,
            @JsonProperty("maturity_ratings")
            var maturityRatings: ArrayList<String> = arrayListOf(),
            @JsonProperty("mature_blocked") var matureBlocked: Boolean? = null,
            @JsonProperty("images") var images: KrunchyImages? = KrunchyImages(),
            @JsonProperty("media_count") var mediaCount: Int? = null,
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("slug_title") var slugTitle: String? = null,
            @JsonProperty("description") var description: String? = null,
            @JsonProperty("is_mature") var isMature: Boolean? = null,
            @JsonProperty("seo_title") var seoTitle: String? = null,
            @JsonProperty("premium_available_date") var premiumAvailableDate: String? = null,
            @JsonProperty("closed_captions_available") var closedCaptionsAvailable: Boolean? = null,
            @JsonProperty("free_available_date") var freeAvailableDate: String? = null,
            @JsonProperty("available_date") var availableDate: String? = null,
            @JsonProperty("media_type") var mediaType: String? = null,
            @JsonProperty("available_offline") var availableOffline: Boolean? = null,
            @JsonProperty("premium_date") var premiumDate: String? = null,
            @JsonProperty("movie_listing_title") var movieListingTitle: String? = null,
            @JsonProperty("duration_ms") var durationMs: Int? = null,
            @JsonProperty("is_premium_only") var isPremiumOnly: Boolean? = null,
            @JsonProperty("listing_id") var listingId: String? = null,
    )

    private fun KrunchyItems.toHome(dubStatus: DubStatus): SearchResponse {
        val img = this.images?.thumbnail?.get(0)?.get(4)?.source
        val seriesID = this.episodeMetadata?.seriesId
        val epNum = this.episodeMetadata?.episode ?: this.episodeMetadata?.episodeNumber
        val data = "{\"tvtype\":\"series\",\"seriesID\":\"$seriesID\"}"
        val title = this.episodeMetadata?.seriesTitle ?: ""
        return newAnimeSearchResponse(title, data) {
            this.posterUrl = img
            addDubStatus(dubStatus, epNum?.toIntOrNull())
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        proxyToken()
        val items = ArrayList<HomePageList>()
        val aaaa =
                app.get(
                                "$krunchyapi/content/v1/browse?n=100&sort_by=newly_added&type=episode",
                                headers = latestKrunchyHeader
                        )
                        .parsed<KrunchyHome>()

        val epss =
                aaaa.items
                        .filter {
                            it.episodeMetadata?.audioLocale == "ja-JP" ||
                                    it.episodeMetadata?.audioLocale?.isEmpty() == true
                        }
                        .map { it.toHome(DubStatus.Subbed) }

        val ssss =
                aaaa.items.filter { it.episodeMetadata?.audioLocale == "en-US" }.map {
                    it.toHome(DubStatus.Dubbed)
                }

        val res =
                app.get(
                                "$krunchyapi/content/v1/browse?locale=en-US&n=30&sort_by=popularity",
                                headers = latestKrunchyHeader
                        )
                        .parsed<KrunchyHome>()

        val home =
                res.items.map {
                    val title = it.title
                    val issub = it.seriesMetadata?.isSubbed == true
                    val isdub = it.seriesMetadata?.isDubbed == true
                    // val epss = it.seriesMetadata?.episodeCount
                    val posterstring = it.images?.posterTall.toString()
                    // val ttt = it.images?.posterTall?.get(0)?.get(6)?.source ?: ""
                    val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                    val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                    val seriesID = it.id
                    val data = "{\"tvtype\":\"${it.type}\",\"seriesID\":\"$seriesID\"}"
                    newAnimeSearchResponse(title!!, data) {
                        this.posterUrl = poster
                        addDubStatus(isdub, issub)
                    }
                }

        if (home.isNotEmpty()) items.add(HomePageList("Popular", home))
        if (epss.isNotEmpty()) items.add(HomePageList("New episodes (SUB)", epss, true))
        if (ssss.isNotEmpty()) items.add(HomePageList("New episodes (DUB)", ssss, true))
        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items)
    }

    data class BetaKronchGEOSearch(
            @JsonProperty("total") var total: Int? = null,
            @JsonProperty("data") var data: ArrayList<GeoSearchData> = arrayListOf(),
    )

    data class GeoSearchData(
            @JsonProperty("items") var items: ArrayList<GeoSearchItems> = arrayListOf()
    )

    data class GeoSearchItems(
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("promo_title") var promoTitle: String? = null,
            @JsonProperty("external_id") var externalId: String? = null,
            @JsonProperty("series_metadata")
            var seriesMetadata: KrunchySeriesMetadata? = KrunchySeriesMetadata(),
            @JsonProperty("type") var type: String? = null,
            @JsonProperty("promo_description") var promoDescription: String? = null,
            @JsonProperty("slug_title") var slugTitle: String? = null,
            @JsonProperty("slug") var slug: String? = null,
            @JsonProperty("channel_id") var channelId: String? = null,
            @JsonProperty("images") var images: KrunchyImages? = KrunchyImages(),
            @JsonProperty("description") var description: String? = null,
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("linked_resource_key") var linkedResourceKey: String? = null,
            @JsonProperty("new") var new: Boolean? = null
    )

    override suspend fun quickSearch(query: String): List<SearchResponse> {
        return search(query)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        proxyToken()
        val url = "$krunchyapi/content/v2/discover/search?q=$query&type=series,movie_listing"
        val search = ArrayList<SearchResponse>()
        val res = app.get(url, headers = latestKrunchyHeader).parsed<BetaKronchGEOSearch>()
        res.data.map {
            it.items.map { info ->
                val title = info.title
                val image = info.images?.posterTall?.get(0)?.get(6)?.source ?: ""
                val dubExist = info.seriesMetadata?.isDubbed ?: false
                val subExist = info.seriesMetadata?.isSubbed ?: false
                val id = info.id
                val type = info.type
                val data = "{\"tvtype\":\"$type\",\"seriesID\":\"$id\"}"
                search.add(
                        newAnimeSearchResponse(title!!, data) {
                            this.posterUrl = image
                            addDubStatus(dubExist, subExist)
                        }
                )
            }
        }

        return search
    }

    data class LoadDataInfo(
            @JsonProperty("tvtype") var tvtype: String? = null,
            @JsonProperty("seriesID") var seriesID: String? = null
    )
    data class Versions(
            @JsonProperty("audio_locale") var audioLocale: String? = null,
            @JsonProperty("guid") var guid: String? = null,
            @JsonProperty("original") var original: Boolean? = null,
            @JsonProperty("variant") var variant: String? = null
    )

    private suspend fun getKronchMetadataInfo(url: String): KrunchyLoadMain {
        proxyToken()
        return app.get(url, headers = latestKrunchyHeader).parsed()
    }

    private suspend fun getMovie(id: String?): ArrayList<Episode> {
        proxyToken()
        val movie = ArrayList<Episode>()
        val metadainfo =
                app.get(
                                "$krunchyapi/content/v2/cms/movie_listings/$id/movies?locale=en-US",
                                headers = latestKrunchyHeader
                        )
                        .parsed<KrunchyLoadMain>()
        metadainfo.data.map {
            val title = it.title
            val epID = it.id
            val epdesc = it.description
            val posterstring = it.images?.thumbnail.toString()
            val issub = it.isSubbed
            val posterRegex =
                    Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
            val poster = posterRegex.find(posterstring)?.destructured?.component1()
            val dataep = "{\"id\":\"$epID\",\"issub\":$issub}"
            movie.add(
                    Episode(
                            name = title,
                            data = dataep,
                            description = epdesc,
                            posterUrl = poster,
                    )
            )
        }
        return movie
    }

    data class BetaKronch(
            @JsonProperty("total") var total: Int? = null,
            @JsonProperty("data") var data: ArrayList<BetaKronchData> = arrayListOf(),
    )
    data class BetaKronchData(
            @JsonProperty("is_complete") var isComplete: Boolean? = null,
            @JsonProperty("keywords") var keywords: ArrayList<String>? = arrayListOf(),
            @JsonProperty("maturity_ratings")
            var maturityRatings: ArrayList<String>? = arrayListOf(),
            @JsonProperty("is_subbed") var isSubbed: Boolean? = null,
            @JsonProperty("audio_locale") var audioLocale: String? = null,
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("series_id") var seriesId: String? = null,
            @JsonProperty("is_simulcast") var isSimulcast: Boolean? = null,
            @JsonProperty("slug_title") var slugTitle: String? = null,
            @JsonProperty("season_number") var seasonNumber: Int? = null,
            @JsonProperty("versions") var versions: ArrayList<Versions>? = arrayListOf(),
            @JsonProperty("free_available_date") var freeAvailableDate: String? = null,
            @JsonProperty("next_episode_id") var nextEpisodeId: String? = null,
            @JsonProperty("premium_date") var premiumDate: String? = null,
            @JsonProperty("episode_number") var episodeNumber: Int? = null,
            @JsonProperty("season_title") var seasonTitle: String? = null,
            @JsonProperty("is_mature") var isMature: Boolean? = null,
            @JsonProperty("streams_link") var streamsLink: String? = null,
            @JsonProperty("available_date") var availableDate: String? = null,
            @JsonProperty("listing_id") var listingId: String? = null,
            @JsonProperty("is_dubbed") var isDubbed: Boolean? = null,
            @JsonProperty("slug") var slug: String? = null,
            @JsonProperty("mature_blocked") var matureBlocked: Boolean? = null,
            @JsonProperty("sequence_number") var sequenceNumber: Int? = null,
            @JsonProperty("is_clip") var isClip: Boolean? = null,
            @JsonProperty("season_slug_title") var seasonSlugTitle: String? = null,
            @JsonProperty("description") var description: String? = null,
            @JsonProperty("eligible_region") var eligibleRegion: String? = null,
            @JsonProperty("media_type") var mediaType: String? = null,
            @JsonProperty("subtitle_locales")
            var subtitleLocales: ArrayList<String>? = arrayListOf(),
            @JsonProperty("episode") var episode: String? = null,
            @JsonProperty("images") var images: KrunchyImages? = KrunchyImages(),
            @JsonProperty("series_title") var seriesTitle: String? = null,
            @JsonProperty("next_episode_title") var nextEpisodeTitle: String? = null,
            @JsonProperty("seo_title") var seoTitle: String? = null,
            @JsonProperty("hd_flag") var hdFlag: Boolean? = null,
            @JsonProperty("available_offline") var availableOffline: Boolean? = null,
            @JsonProperty("series_slug_title") var seriesSlugTitle: String? = null,
            @JsonProperty("availability_ends") var availabilityEnds: String? = null,
            @JsonProperty("production_episode_id") var productionEpisodeId: String? = null,
            @JsonProperty("identifier") var identifier: String? = null,
            @JsonProperty("episode_air_date") var episodeAirDate: String? = null,
            @JsonProperty("upload_date") var uploadDate: String? = null,
            @JsonProperty("availability_starts") var availabilityStarts: String? = null,
            @JsonProperty("premium_available_date") var premiumAvailableDate: String? = null,
            @JsonProperty("availability_notes") var availabilityNotes: String? = null,
            @JsonProperty("season_id") var seasonId: String? = null,
            @JsonProperty("duration_ms") var durationMs: Int? = null,
            @JsonProperty("closed_captions_available") var closedCaptionsAvailable: Boolean? = null,
            @JsonProperty("seo_description") var seoDescription: String? = null,
            @JsonProperty("is_premium_only") var isPremiumOnly: Boolean? = null
    )

    data class BetaKronchRecsMain(
            @JsonProperty("total") var total: Int? = null,
            @JsonProperty("data") var data: ArrayList<BetaKronchRecData> = arrayListOf(),
    )
    data class BetaKronchRecData(
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("slug_title") var slugTitle: String? = null,
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("promo_title") var promoTitle: String? = null,
            @JsonProperty("description") var description: String? = null,
            @JsonProperty("type") var type: String? = null,
            @JsonProperty("images") var images: KrunchyImages? = KrunchyImages(),
            @JsonProperty("promo_description") var promoDescription: String? = null,
            @JsonProperty("new") var new: Boolean? = null,
            @JsonProperty("slug") var slug: String? = null,
            @JsonProperty("channel_id") var channelId: String? = null,
            @JsonProperty("linked_resource_key") var linkedResourceKey: String? = null,
            @JsonProperty("external_id") var externalId: String? = null,
    )

    private suspend fun getRecommendations(seriesId: String?): List<SearchResponse>? {
        proxyToken()
        val recsurl = "$krunchyapi/content/v2/discover/similar_to/$seriesId?locale=en-US&n=30"
        val res = app.get(recsurl, headers = latestKrunchyHeader).parsedSafe<BetaKronchRecsMain>()
        return res?.data?.map { rec ->
            val sID = rec.id
            val rTitle = rec.title ?: ""
            val poster = rec.images?.posterTall?.map { it[5].source }?.first() ?: ""
            val rType = rec.type
            val rdata = "{\"tvtype\":\"$rType\",\"seriesID\":\"$sID\"}"
            newAnimeSearchResponse(rTitle, rdata) { this.posterUrl = poster }
        }
    }

    private fun BetaKronchData.togetNormalEps(isSubbed: Boolean?): Episode {
        val eptitle = this.title
        val epID =
                this.streamsLink?.substringAfter("/videos/")?.substringBefore("/streams") ?: this.id
        val epthumb = this.images?.thumbnail?.map { it[3].source }?.first() ?: ""
        val epplot = this.description
        val season = this.seasonNumber
        val epnum = this.episodeNumber
        val dataep = "{\"id\":\"$epID\",\"issub\":$isSubbed}"
        val sstitle = this.seasonTitle ?: ""
        // To fix missing seasons
        val newSeason =
                if (sstitle.contains(
                                Regex(
                                        "(?i)(Alicization War of Underworld|Battle of Kimluck|Entertainment District Arc|Swordsmith Village)"
                                )
                        )
                )
                        season?.plus(1)
                else if (epnum == null) 0 else season

        val date = this.episodeAirDate

        return newEpisode(dataep) {
            this.name = eptitle
            this.description = epplot
            this.posterUrl = epthumb
            this.season = newSeason
            this.episode = epnum
            addDate(date, format = "yyyy-MM-dd'T'HH:mm:ss")
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val fixedData = url.replace("https://www.crunchyroll.com/", "")
        val parsedData = parseJson<LoadDataInfo>(fixedData)
        val seriesIDSuper = parsedData.seriesID
        val type = parsedData.tvtype
        val tvType = if (type!!.contains("movie")) TvType.AnimeMovie else TvType.Anime
        val isMovie = tvType == TvType.AnimeMovie
        val tttt =
                if (isMovie) "$krunchyapi/content/v2/cms/movie_listings/$seriesIDSuper?locale=en-US"
                else "$krunchyapi/content/v2/cms/series/$seriesIDSuper?locale=en-US"
        val response = getKronchMetadataInfo(tttt).data.first()
        val dubEps = ArrayList<Episode>()
        val subEps = if (isMovie) getMovie(seriesIDSuper) else ArrayList()
        val title = response.title.toString()
        val plot = response.description.toString()
        val year = response.seriesLaunchYear
        val poster = response.images?.posterTall?.get(0)?.get(6)?.source ?: ""
        val backposter = response.images?.posterWide?.get(0)?.get(7)?.source ?: ""
        val tags = response.keywords
        val infodata = "{\"tvtype\":\"$type\",\"seriesID\":\"$seriesIDSuper\"}"
        val recommendations = getRecommendations(seriesIDSuper)
        if (tvType == TvType.Anime) {
            proxyToken()
            val nn =
                    app.get(
                                    "$krunchyapi/content/v2/cms/series/$seriesIDSuper/seasons?locale=en-US",
                                    headers = latestKrunchyHeader
                            )
                            .parsedSafe<BetaKronch>()
                            ?: throw ErrorLoadingException("Couldn't get episodes, try again")
            if (nn.data.isEmpty()) throw ErrorLoadingException("Failed to get episodes, try again")
            val inn =
                    nn.data.filter {
                        !it.title!!.contains(
                                Regex("Piece: East Blue|Piece: Alabasta|Piece: Sky Island")
                        )
                        // || it.audioLocale == "ja-JP" || it.audioLocale == "zh-CN" ||
                        // it.audioLocale
                        // == "en-US" || it.audioLocale?.isEmpty() == true
                    }
            val innversions =
                    inn.filter {
                        !it.versions.isNullOrEmpty() ||
                                it.audioLocale == "ja-JP" ||
                                it.audioLocale == "zh-CN" ||
                                it.audioLocale == "en-US" ||
                                it.audioLocale?.isEmpty() == true
                    }

            inn.apmap { main ->
                val mainID = main.id
                val res =
                        app.get(
                                        "$krunchyapi/content/v2/cms/seasons/$mainID/episodes?&locale=en-US",
                                        headers = latestKrunchyHeader
                                )
                                .parsedSafe<BetaKronch>()
                                ?: throw ErrorLoadingException("Couldn't get episodes, try again")
                if (res.data.isEmpty())
                        throw ErrorLoadingException(
                                "Failed to load season ${main.title}, try again"
                        )
                val restwo =
                        res.data.filter {
                            it.audioLocale == "ja-JP" ||
                                    it.audioLocale == "zh-CN" ||
                                    it.audioLocale?.isEmpty() == true
                        }
                restwo.map { second ->
                    val clip = second.isClip == false
                    if (clip) {
                        subEps.add(second.togetNormalEps(true))
                    }
                }
            }
            innversions.map { ve ->
                val versionsfiltered =
                        ve.versions?.filter {
                            (it.audioLocale?.contains(Regex("ja-JP|en-US|zh-CN")) == true ||
                                    it.audioLocale.isNullOrEmpty())
                        }
                versionsfiltered?.apmap { vers ->
                    val guid = vers.guid
                    val resv =
                            app.get(
                                            "$krunchyapi/content/v2/cms/seasons/$guid/episodes?&locale=en-US",
                                            headers = latestKrunchyHeader
                                    )
                                    .parsedSafe<BetaKronch>()
                                    ?: throw ErrorLoadingException(
                                            "Couldn't get episodes, try again"
                                    )
                    if (resv.data.isEmpty())
                            throw ErrorLoadingException(
                                    "Failed to load season ${ve.title}, try again"
                            )
                    resv.data.map { pss ->
                        val clip = pss.isClip == false
                        val audioss = pss.audioLocale
                        if (clip) {
                            if (audioss == "en-US") {
                                val dubss = pss.togetNormalEps(false)
                                dubEps.add(dubss)
                            }
                            if (audioss.isNullOrEmpty() || audioss == "ja-JP" || audioss == "zh-CN"
                            ) {
                                val subbs = pss.togetNormalEps(true)
                                subEps.add(subbs)
                            }
                        }
                    }
                }
            }
        }

        val sases =
                subEps.distinctBy { it.data }.sortedWith(compareBy({ it.season }, { it.episode }))

        val dubes =
                dubEps.distinctBy { it.data }.sortedWith(compareBy({ it.season }, { it.episode }))

        return newAnimeLoadResponse(title, infodata, TvType.Anime) {
            if (subEps.isNotEmpty()) addEpisodes(DubStatus.Subbed, sases.toSet().toList())
            if (dubEps.isNotEmpty()) addEpisodes(DubStatus.Dubbed, dubes.toSet().toList())
            this.plot = plot
            this.tags = tags
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backposter
            this.recommendations = recommendations
        }
    }

    data class EpsInfo(
            @JsonProperty("id") var id: String? = null,
            @JsonProperty("issub") var issub: Boolean? = null
    )

    private suspend fun getKronchStream(
            streamLink: String,
            name: String,
            callback: (ExtractorLink) -> Unit
    ) {
        return M3u8Helper.generateM3u8(name, streamLink, "https://static.crunchyroll.com/")
                .forEach(callback)
    }

    data class Subtitle(
            val locale: String? = null,
            val url: String? = null,
            val format: String? = null,
    )

    data class Testt(
            @JsonProperty("adaptive_hls") val adaptiveHLS: Map<String, BetaKronchS>? = null,
            @JsonProperty("vo_adaptive_hls") val vrvHLS: Map<String, BetaKronchS>? = null,
            @JsonProperty("multitrack_adaptive_hls_v2")
            val multiadaptiveHLS: Map<String, BetaKronchS>? = null,
    )

    data class BetaKronchS(
            @JsonProperty("hardsub_locale") var hardsubLocale: String? = null,
            @JsonProperty("url") var url: String? = null,
    )

    data class BetaKronchGEOStreams(
            @JsonProperty("total") var total: Int? = null,
            @JsonProperty("data") var data: ArrayList<Testt>? = arrayListOf(),
            @JsonProperty("meta") var meta: Meta? = Meta()
    )
    data class Meta(
            @JsonProperty("subtitles") var subtitles: Map<String, Subtitle>? = null,
    )

    private fun fixLocale(locale: String?): String {
        val localea =
                when (locale) {
                    "ja-JP" -> "Japanese"
                    "en-US" -> "English"
                    "de-DE" -> "German"
                    "es-ES" -> "Spanish"
                    "es-419" -> "Spanish LAT"
                    "fr-FR" -> "French"
                    "it-IT" -> "Italian"
                    "pt-BR" -> "Portuguese (Brazil)"
                    "pt-PT" -> "Portuguese (Portugal)"
                    "ru-RU" -> "Russian"
                    "zh-CN" -> "Chinese (Simplified)"
                    "tr-TR" -> "Turkish"
                    "ar-ME" -> "Arabic"
                    "ar-SA" -> "Arabic (Saudi Arabia)"
                    "uk-UK" -> "Ukrainian"
                    "he-IL" -> "Hebrew"
                    "pl-PL" -> "Polish"
                    "ro-RO" -> "Romanian"
                    "sv-SE" -> "Swedish"
                    "" -> ""
                    else -> "[$locale] "
                }
        return localea
    }
    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        proxyToken()
        val newdata = data.replace("https://www.crunchyroll.com/", "")
        val parsedata = parseJson<EpsInfo>(newdata)
        val mediaId = parsedata.id
        val issub = parsedata.issub == true
        val response =
                app.get("$krunchyapi/content/v2/cms/videos/$mediaId/streams", latestKrunchyHeader)
                        .parsed<BetaKronchGEOStreams>()

        response.data?.map { testt ->
            val adphls = testt.multiadaptiveHLS ?: testt.adaptiveHLS
            val vvhls = testt.vrvHLS
            val bbb = listOfNotNull(vvhls, adphls)
            bbb.apmap { aa ->
                aa.entries.filter { it.key == "en-US" || it.key.isEmpty() }.map { it.value }.amap {
                        str ->
                    val m3u8Url = str.url
                    val testtting =
                            app.get(m3u8Url!!, referer = "https://static.crunchyroll.com/").text
                    if (testtting.contains(Regex("(?i)accessdenied")))
                            handler.postFunction {
                                context.let { tt ->
                                    Toast.makeText(tt, "Reload links.", Toast.LENGTH_LONG).show()
                                }
                            }
                    else {
                        val raw = str.hardsubLocale?.isEmpty()
                        val hardsubinfo = str.hardsubLocale?.contains("en-US")
                        val vvv = if (str.url!!.contains("vrv.co")) "_VRV" else ""
                        val name =
                                if (raw == false && issub) "Kronch$vvv Hardsub English (US)"
                                else if (raw == true && issub) "Kronch$vvv RAW"
                                else "Kronch$vvv English"
                        if (hardsubinfo == true && issub) {
                            getKronchStream(str.url!!, name, callback)
                        }
                        if (raw == true) {
                            getKronchStream(str.url!!, name, callback)
                        }
                    }
                }
            }
        }
        response.meta?.subtitles?.map { it.value }?.map {
            val lang = fixLocale(it.locale)
            val url = it.url
            subtitleCallback.invoke(SubtitleFile(lang, url!!))
        }
        return true
    }
}
