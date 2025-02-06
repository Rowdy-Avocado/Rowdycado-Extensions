package com.RowdyAvocado

import com.RowdyAvocado.CodeExtractor.invokeAnitaku
import com.RowdyAvocado.CodeExtractor.invokeMoviesmod
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.argamap
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink

class CodeStreamTest : CodeStream() {
    override var name = "CodeStream-Test"
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val res = AppUtils.parseJson<LinkData>(data)
        Log.d("Test1", "$res")
        argamap(
            {   if (res.isAnime) invokeAnitaku(
                res.title,
                res.epsTitle,
                res.date,
                res.year,
                res.season,
                res.episode,
                subtitleCallback,
                callback
            )
            },
            {
                if (!res.isAnime && !res.isBollywood) invokeMoviesmod(
                    res.title,
                    res.year,
                    res.season,
                    res.lastSeason,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            }
        )
        return true
    }

}