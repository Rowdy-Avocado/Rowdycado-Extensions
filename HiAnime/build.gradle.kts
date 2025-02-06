// use an integer for version numbers
import org.jetbrains.kotlin.konan.properties.Properties

version = 14

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val properties = Properties()
        android.buildFeatures.buildConfig=true
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "HIANIMEAPI", "\"${properties.getProperty("HIANIMEAPI")}\"")
    }
}


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    //description = "Webview is used to load links, reload if necessary"
    authors = listOf("Stormunblessed, KillerDogeEmpire,RowdyRushya,Phisher98")

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

    iconUrl = "https://www.google.com/s2/favicons?domain=hianime.to&sz=%size%"

    isCrossPlatform = false
}
