rootProject.buildFileName = "build.gradle.kts"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
rootProject.name = "Zotero Android"
include("app")