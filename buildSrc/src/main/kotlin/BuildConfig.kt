object BuildConfig {
    const val appId = "org.zotero.android"
    const val minSdkVersion = 23
    const val compileSdkVersion = 34
    const val targetSdk = 33

    val versionCode = 26 // Must be updated on every build
    val version = Version(
        major = 1,
        minor = 0,
        patch = 13,
    )
}

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    val name = "$major.$minor.$patch"
}
