package com.RowdyAvocado

import com.RowdyAvocado.CodeExtractor.invoke2embed
import com.RowdyAvocado.CodeExtractor.invokeAllMovieland
import com.RowdyAvocado.CodeExtractor.invokeAnimes
import com.RowdyAvocado.CodeExtractor.invokeAoneroom
import com.RowdyAvocado.CodeExtractor.invokeBollyflix
import com.RowdyAvocado.CodeExtractor.invokeDoomovies
import com.RowdyAvocado.CodeExtractor.invokeDramaday
import com.RowdyAvocado.CodeExtractor.invokeDreamfilm
import com.RowdyAvocado.CodeExtractor.invokeFilmxy
import com.RowdyAvocado.CodeExtractor.invokeFlixon
import com.RowdyAvocado.CodeExtractor.invokeKimcartoon
import com.RowdyAvocado.CodeExtractor.invokeKisskh
import com.RowdyAvocado.CodeExtractor.invokeLing
import com.RowdyAvocado.CodeExtractor.invokeM4uhd
import com.RowdyAvocado.CodeExtractor.invokeNinetv
import com.RowdyAvocado.CodeExtractor.invokeNowTv
import com.RowdyAvocado.CodeExtractor.invokeRidomovies
//import com.RowdyAvocado.CodeExtractor.invokeSmashyStream
import com.RowdyAvocado.CodeExtractor.invokeDumpStream
import com.RowdyAvocado.CodeExtractor.invokeEmovies
import com.RowdyAvocado.CodeExtractor.invokeMultimovies
import com.RowdyAvocado.CodeExtractor.invokeNetmovies
import com.RowdyAvocado.CodeExtractor.invokeShowflix
import com.RowdyAvocado.CodeExtractor.invokeVidSrc
import com.RowdyAvocado.CodeExtractor.invokeVidsrcto
import com.RowdyAvocado.CodeExtractor.invokeCinemaTv
import com.RowdyAvocado.CodeExtractor.invokeMoflix
import com.RowdyAvocado.CodeExtractor.invokeGhostx
//import com.RowdyAvocado.CodeExtractor.invokeNepu
import com.RowdyAvocado.CodeExtractor.invokeWatchCartoon
import com.RowdyAvocado.CodeExtractor.invokeWatchsomuch
import com.RowdyAvocado.CodeExtractor.invokeZoechip
import com.RowdyAvocado.CodeExtractor.invokeZshow
import com.RowdyAvocado.CodeExtractor.invokeMoviesdrive
import com.RowdyAvocado.CodeExtractor.invokeVegamovies
import com.RowdyAvocado.CodeExtractor.invokeDotmovies
import com.RowdyAvocado.CodeExtractor.invokeTopMovies
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.argamap
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink

class CodeStreamLite : CodeStream() {
    override var name = "StreamPlay-Lite"

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val res = AppUtils.parseJson<LinkData>(data)

        argamap(
            { if (!res.isAnime)
                invokeM4uhd(res.title, res.airedYear?: res.year, res.season, res.episode,subtitleCallback,callback)
                invokeBollyflix(res.title,res.year,res.season,res.lastSeason,res.episode,subtitleCallback,callback)
                invokeMoflix(res.id, res.season, res.episode, callback)
                invokeWatchsomuch(res.imdbId,res.season,res.episode,subtitleCallback)
                invokeMoviesdrive(res.title,res.season,res.episode,res.year,subtitleCallback,callback)
                invokeTopMovies(res.title,res.year,res.season,res.lastSeason,res.episode,subtitleCallback,callback)
            },
            {
                invokeDumpStream(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeNinetv(
                    res.id,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokeVidSrc(res.id, res.season, res.episode, subtitleCallback,callback)
            },
            {
                if (!res.isAnime && res.isCartoon) invokeWatchCartoon(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (res.isAnime) invokeAnimes(
                    res.title,
                    res.epsTitle,
                    res.date,
                    res.airedDate,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeDreamfilm(
                    res.title,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeFilmxy(
                    res.imdbId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeGhostx(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    callback
                )
            },
            {
                if (!res.isAnime && res.isCartoon) invokeKimcartoon(
                    res.title,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            /*    {
                    if (!res.isAnime) invokeSmashyStream(
                        res.id,
                        res.season,
                        res.episode,
                        subtitleCallback,
                        callback
                    )
                },
             */
            {
                if (!res.isAnime) invokeVidsrcto(
                    res.imdbId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (res.isAsian || res.isAnime) invokeKisskh(
                    res.title,
                    res.season,
                    res.episode,
                    res.isAnime,
                    res.lastSeason,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeLing(
                    res.title, res.airedYear
                        ?: res.year, res.season, res.episode, subtitleCallback, callback
                )
            },
            /*{
                if (!res.isAnime) invokeM4uhd(
                    res.title, res.airedYear
                        ?: res.year, res.season, res.episode, subtitleCallback, callback
                )
            },
             */
            {
                if (!res.isAnime) invokeFlixon(
                    res.id,
                    res.imdbId,
                    res.season,
                    res.episode,
                    callback
                )
            },
            {
                invokeCinemaTv(
                    res.imdbId, res.title, res.airedYear
                        ?: res.year, res.season, res.episode, subtitleCallback, callback
                )
            },
            {
                if (!res.isAnime) invokeNowTv(res.id, res.imdbId, res.season, res.episode, callback)
            },
            {
                if (!res.isAnime) invokeAoneroom(
                    res.title, res.airedYear
                        ?: res.year, res.season, res.episode, subtitleCallback, callback
                )
            },
            {
                if (!res.isAnime) invokeRidomovies(
                    res.id,
                    res.imdbId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeEmovies(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokeMultimovies(
                    multimoviesAPI,
                    res.title,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokeNetmovies(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeAllMovieland(res.imdbId, res.season, res.episode, callback)
            },
            {
                if (!res.isAnime && res.season == null) invokeDoomovies(
                    res.title,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (res.isAsian) invokeDramaday(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invoke2embed(
                    res.imdbId,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                invokeZshow(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeShowflix(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeZoechip(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    callback
                )
            },
            /*{
                if (!res.isAnime) invokeNepu(
                    res.title,
                    res.airedYear ?: res.year,
                    res.season,
                    res.episode,
                    callback
                )
            }

             */
            {
                if (!res.isAnime) invokeVegamovies(
                    res.title,
                    res.year,
                    res.season,
                    res.lastSeason,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if (!res.isAnime) invokeDotmovies(
                    res.title,
                    res.year,
                    res.season,
                    res.lastSeason,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
        )
        return true
    }

}