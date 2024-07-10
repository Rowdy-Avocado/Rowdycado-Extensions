version = 7


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Audiobook Source with good library, use Search, the homepage sucks"
    authors = listOf("KillerDogeEmpire")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "Others"
    )

    iconUrl = "https://tokybook.com/wp-content/uploads/TOKY-BOOK-website.png"
}
