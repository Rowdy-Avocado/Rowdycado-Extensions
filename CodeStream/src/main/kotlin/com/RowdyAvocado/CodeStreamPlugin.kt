package com.RowdyAvocado

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.DoodYtExtractor
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.extractors.Gofile
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.extractors.Moviesapi
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidHidePro6
import com.lagradost.cloudstream3.extractors.VidSrcExtractor
import com.lagradost.cloudstream3.extractors.Vidmolyme
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.extractors.Voe

@CloudstreamPlugin
class CodeStreamPlugin: BasePlugin() {
    override fun load() {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(CodeStream())
        registerMainAPI(CodeStreamLite())
        registerMainAPI(CodeStreamTest())
        registerExtractorAPI(Animefever())
        registerExtractorAPI(Multimovies())
        registerExtractorAPI(MultimoviesSB())
        registerExtractorAPI(Yipsu())
        registerExtractorAPI(Mwish())
        registerExtractorAPI(TravelR())
        registerExtractorAPI(Playm4u())
        registerExtractorAPI(Vidplay())
        registerExtractorAPI(FileMoon())
        registerExtractorAPI(VCloud())
        registerExtractorAPI(Bestx())
        registerExtractorAPI(Filelions())
        registerExtractorAPI(Snolaxstream())
        registerExtractorAPI(Pixeldra())
        registerExtractorAPI(Mp4Upload())
        registerExtractorAPI(Graceaddresscommunity())
        registerExtractorAPI(M4ufree())
        registerExtractorAPI(Streamruby())
        registerExtractorAPI(StreamWishExtractor())
        registerExtractorAPI(Filelion())
        registerExtractorAPI(DoodYtExtractor())
        registerExtractorAPI(dlions())
        registerExtractorAPI(MixDrop())
        registerExtractorAPI(dwish())
        registerExtractorAPI(Embedwish())
        registerExtractorAPI(UqloadsXyz())
        registerExtractorAPI(Uploadever())
        registerExtractorAPI(Netembed())
        registerExtractorAPI(Flaswish())
        registerExtractorAPI(Comedyshow())
        registerExtractorAPI(Ridoo())
        registerExtractorAPI(Streamvid())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(do0od())
        registerExtractorAPI(Embedrise())
        registerExtractorAPI(Gdmirrorbot())
        registerExtractorAPI(FilemoonNl())
        registerExtractorAPI(Alions())
        registerExtractorAPI(Vidmolyme())
        registerExtractorAPI(AllinoneDownloader())
        registerExtractorAPI(Tellygossips())
        registerExtractorAPI(Tvlogy())
        registerExtractorAPI(Voe())
        registerExtractorAPI(Mdrive())
        registerExtractorAPI(Gofile())
        registerExtractorAPI(Animezia())
        registerExtractorAPI(Moviesapi())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(Modflix())
        registerExtractorAPI(Vectorx())
        registerExtractorAPI(Sethniceletter())
        registerExtractorAPI(GDFlix())
        registerExtractorAPI(fastdlserver())
        registerExtractorAPI(Asianbxkiun())
        registerExtractorAPI(GDFlix1())
        registerExtractorAPI(GDFlix2())
        registerExtractorAPI(furher())
        registerExtractorAPI(VidSrcExtractor())
        registerExtractorAPI(Servertwo())
        registerExtractorAPI(MultimoviesAIO())
        registerExtractorAPI(HubCloud())
        registerExtractorAPI(HubCloudClub())
        registerExtractorAPI(HubCloudlol())
        registerExtractorAPI(Driveseed())
        registerExtractorAPI(Driveleech())
        registerExtractorAPI(VidHidePro6())
        registerExtractorAPI(MixDropSi())
        registerExtractorAPI(Mp4Upload())
    }
}
