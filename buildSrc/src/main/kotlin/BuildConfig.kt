object BuildConfig {
    const val appId = "org.zotero.android"
    const val minSdkVersion = 23
    const val compileSdkVersion = 34
    const val targetSdk = 34

    val versionCode = 108 // Must be updated on every build
    val version = Version(
        major = 1,
        minor = 0,
        patch = 0,
        versionCode = versionCode,
    )
}

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val versionCode: Int,
) {
    val name = "$major.$minor.$patch-$versionCode"
}
