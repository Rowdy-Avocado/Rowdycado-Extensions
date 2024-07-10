dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
// use an integer for version numbers
version = 10


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Anime from all-wish.me"
    authors = listOf("RowdyRushya")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1

    tvTypes = listOf("All")

    requiresResources = true
    language = "en"

    // random cc logo i found
    iconUrl = "https://manga.all-wish.me/favicon.ico"
}

android {
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        minSdk = 26
        compileSdkVersion(33)
        targetSdk = 33
    }
}
