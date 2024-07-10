// use an integer for version numbers
version = 33


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "With this extension you will be able to watch english hardsubbed/dubbed anime available in the US catalog of Crunchyroll."
    authors = listOf("Stormunblessed, KillerDogeEmpire")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://raw.githubusercontent.com/Stormunblessed/IPTV-CR-NIC/main/logos/kronch.png"
}
