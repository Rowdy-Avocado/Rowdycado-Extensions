package com.KillerDogeEmpire

import android.util.Log
import com.KillerDogeEmpire.CodeExtractor.invokeAnitaku
import com.KillerDogeEmpire.CodeExtractor.invokeBollyflix
import com.KillerDogeEmpire.CodeExtractor.invokeMoviesmod
import com.KillerDogeEmpire.CodeExtractor.invokeVegamovies
import com.KillerDogeEmpire.CodeExtractor.invokeMoviesdrive
import com.KillerDogeEmpire.CodeExtractor.invokeTopMovies
import com.KillerDogeEmpire.CodeExtractor.invokeUhdmovies
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