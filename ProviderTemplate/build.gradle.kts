// use an integer for version numbers
version = -1

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Lorem ipsum"
    authors = listOf("Template")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
     // example "Anime",
    )

    // random cc logo
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"

    requiresResources = true
}

android {
    buildFeatures {
        viewBinding = true
    }
}
