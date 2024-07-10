package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app

class MangaDexExtractor(val plugin: MangaDexPlugin) {
    suspend fun getPages(chapter: ChapterData): List<String> {
        var pages = emptyList<String>()
        if (chapter.attrs.externalUrl.isNullOrBlank()) {
            val chapterPages =
                    app.get(
                                    "https://api.mangadex.org/at-home/server/${chapter.id}?forcePort443=false"
                            )
                            .parsedSafe<MangaDexChapterPagesResponse>()!!
            val prefix: String
            val images: List<String>
            if (plugin.dataSaver) {
                prefix = "${chapterPages.baseUrl}/data-saver/${chapterPages.chapter.hash}/"
                images = chapterPages.chapter.dataSaver
            } else {
                prefix = "${chapterPages.baseUrl}/data/${chapterPages.chapter.hash}/"
                images = chapterPages.chapter.data
            }
            pages = images.mapNotNull { image -> prefix + image }
        } else {
            when (chapter.rel.find { it.type.contains("scanlation_group") }?.attrs?.name) {
                "Webnovel" -> {}
                "Pocket Comics" -> {}
                "TappyToon" -> {}
                else -> {}
            }
        }

        return pages
    }
}

data class MangaDexChapterPagesResponse(
        @JsonProperty("result") var result: String,
        @JsonProperty("baseUrl") var baseUrl: String,
        @JsonProperty("chapter") var chapter: MangaDexChapterImages,
)

data class MangaDexChapterImages(
        @JsonProperty("hash") var hash: String,
        @JsonProperty("data") var data: List<String>,
        @JsonProperty("dataSaver") var dataSaver: List<String>,
)
