import org.jetbrains.kotlin.konan.properties.Properties

// use an integer for version numbers
version = 7


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Watch movies and series from Flixerz"
    authors = listOf("RowdyRushya,Phisher98")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1

    tvTypes = listOf("Movies", "TV Series")

    language = "en"

    // random cc logo i found
    iconUrl = "https://myflixerz.to/images/group_1/theme_7/logo.png?v=0.1"

    // Because we use android.graphics.BitmapFactory
    isCrossPlatform = false
}

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        android.buildFeatures.buildConfig=true
        buildConfigField("String", "WASMAPI", "\"${properties.getProperty("WASMAPI")}\"")
        buildConfigField("String", "Proxy", "\"${properties.getProperty("Proxy")}\"")
    }
}
