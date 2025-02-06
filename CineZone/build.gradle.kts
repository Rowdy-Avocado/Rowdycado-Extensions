version = 9

cloudstream {
    description = "The best extension for watching movies and tv shows from CineZone"
    authors = listOf("RowdyRushya")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 0

    tvTypes = listOf("movies", "TvSeries")

    requiresResources = true
    language = "en"

    iconUrl = "https://cinezone.to/assets/sites/cinezone/favicon.png"

    isCrossPlatform = true
}
