object BuildConfig {
    const val appId = "org.zotero.android"
    const val minSdkVersion = 23
    const val compileSdkVersion = 33
    const val targetSdk = 33

    val versionCode = 12 // Must be updated on every build
    val version = Version(
        major = 1,
        minor = 14,
        patch = 0,
    )
}

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    val name = "$major.$minor.$patch"
}
