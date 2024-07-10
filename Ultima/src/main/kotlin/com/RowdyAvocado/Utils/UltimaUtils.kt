package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty

object UltimaUtils {
    data class SectionInfo(
            @JsonProperty("name") var name: String,
            @JsonProperty("url") var url: String,
            @JsonProperty("pluginName") var pluginName: String,
            @JsonProperty("enabled") var enabled: Boolean = false,
            @JsonProperty("priority") var priority: Int = 0
    )

    data class ExtensionInfo(
            @JsonProperty("name") var name: String? = null,
            @JsonProperty("sections") var sections: Array<SectionInfo>? = null
    )

    enum class Category {
        ANIME,
        MEDIA,
        NONE
    }

    data class MediaProviderState(
            @JsonProperty("name") var name: String,
            @JsonProperty("enabled") var enabled: Boolean = true,
            @JsonProperty("customDomain") var customDomain: String? = null
    ) {
        fun getProvider(): MediaProvider {
            return UltimaMediaProvidersUtils.mediaProviders.find { it.name.equals(name) }
                    ?: throw Exception("Unable to find media provider for $name")
        }

        fun getDomain(): String {
            return customDomain ?: getProvider().domain
        }
    }

    data class LinkData(
            @JsonProperty("simklId") val simklId: Int? = null,
            @JsonProperty("traktId") val traktId: Int? = null,
            @JsonProperty("imdbId") val imdbId: String? = null,
            @JsonProperty("tmdbId") val tmdbId: Int? = null,
            @JsonProperty("tvdbId") val tvdbId: Int? = null,
            @JsonProperty("type") val type: String? = null,
            @JsonProperty("season") val season: Int? = null,
            @JsonProperty("episode") val episode: Int? = null,
            @JsonProperty("aniId") val aniId: String? = null,
            @JsonProperty("malId") val malId: String? = null,
            @JsonProperty("title") val title: String? = null,
            @JsonProperty("year") val year: Int? = null,
            @JsonProperty("orgTitle") val orgTitle: String? = null,
            @JsonProperty("isAnime") val isAnime: Boolean = false,
            @JsonProperty("airedYear") val airedYear: Int? = null,
            @JsonProperty("lastSeason") val lastSeason: Int? = null,
            @JsonProperty("epsTitle") val epsTitle: String? = null,
            @JsonProperty("jpTitle") val jpTitle: String? = null,
            @JsonProperty("date") val date: String? = null,
            @JsonProperty("airedDate") val airedDate: String? = null,
            @JsonProperty("isAsian") val isAsian: Boolean = false,
            @JsonProperty("isBollywood") val isBollywood: Boolean = false,
            @JsonProperty("isCartoon") val isCartoon: Boolean = false,
    )
}
