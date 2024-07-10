version = 4


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Audiobooks with some more books not in other sources -  download in addition to GoldenAudiobooks if book not found there"
    authors = listOf("KillerDogeEmpire")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Others"
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=appaudiobooks.com/&sz=%size%"
}
