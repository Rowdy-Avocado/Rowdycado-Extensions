dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
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

    isCrossPlatform = false
}

android {
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        minSdk = 21
        compileSdk = 35
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = 35
    }

    lint {
        targetSdk = 35
    }
}
