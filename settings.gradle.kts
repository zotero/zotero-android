rootProject.buildFileName = "build.gradle.kts"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        kotlin("android") version "2.4.10"
        kotlin("kapt") version "2.4.10"
        kotlin("plugin.serialization") version "2.4.10"
        kotlin("plugin.compose") version "2.4.10"
    }
}
rootProject.name = "Zotero"
include("app")