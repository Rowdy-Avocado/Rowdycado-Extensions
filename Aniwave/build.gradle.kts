// use an integer for version numbers
version = 57

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Watch Aniwave/9anime, I have had reports saying homepage doesn't work the first time but retrying should fix it"
    authors = listOf("RowdyRushya, Horis, Stormunblessed, KillerDogeEmpire, Enimax, Chokerman")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=aniwave.to&sz=%size%"
    
    requiresResources = true
}

dependencies {
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.preference:preference:1.2.1")
}
android {
    buildFeatures {
        viewBinding = true
    }
}
