// use an integer for version numbers
version = 10


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Anime from Anitaku (formerly GoGo Anime)"
    authors = listOf("Cloudburst, Aryan Invader, KillerDogeEmpire, RowdyRushya")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "AnimeMovie",
        "Anime",
        "OVA",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=anitaku.to&sz=%size%"
}
