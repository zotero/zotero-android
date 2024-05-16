import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.github.triplet.play") version "3.7.0"
    id("dagger.hilt.android.plugin")
    id("realm-android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    // Dependency plugins
    id("kotlinDependencies")
    id("androidXDependencies")
    id("composeDependencies")
    id("accompanistDependencies")
    id("daggerAndHiltDependencies")
    id("networkDependencies")
    id("testDependencies")
}

android {
    compileSdk = BuildConfig.compileSdkVersion
    namespace = "org.zotero.android"

    defaultConfig {
        applicationId = BuildConfig.appId
        minSdk = BuildConfig.minSdkVersion
        targetSdk = BuildConfig.targetSdk
        versionCode = BuildConfig.versionCode
        versionName = BuildConfig.version.name
        testInstrumentationRunner = Libs.androidJUnitRunner

        buildConfigField("String", "BASE_API_URL", "\"https://api.zotero.org\"")
        buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
        buildConfigField("String", "PSPDFKIT_KEY", "\"\"")
        manifestPlaceholders["enableCrashReporting"] = false

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("zotero.release.keystore")
            if (rootProject.file("keystore-secrets.txt").exists()) {
                val secrets = rootProject.file("keystore-secrets.txt").readLines()
                keyAlias = secrets[0]
                storePassword = secrets[1]
                keyPassword = secrets[2]
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    androidResources {
        noCompress += listOf("ttf", "mov", "avi", "json", "html", "csv", "obb")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
            manifestPlaceholders["enableCrashReporting"] = false
            extra["enableCrashlytics"] = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs["release"]
            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
            manifestPlaceholders["enableCrashReporting"] = true
        }
    }

    setDefaultProductFlavors()

    productFlavors {
        create("dev") {
            resValue("string", "app_name", "Zotero Debug")
            buildConfigField("String", "PSPDFKIT_KEY", readPspdfkitKey())
            applicationIdSuffix = ".debug"
        }
        create("internal") {
            resValue("string", "app_name", "Zotero Beta")
            buildConfigField("String", "PSPDFKIT_KEY", readPspdfkitKey())
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.pickFirsts.add("META-INF/kotlinx-coroutines-core.kotlin_module")
    }

    androidComponents {
        beforeVariants { variantBuilder ->
            variantBuilder.enable = !variantBuilder.buildType == "release"
        }
    }
}

play {
    track.set("internal")
    defaultToAppBundles.set(true)
    resolutionStrategy.set(ResolutionStrategy.AUTO)
}

dependencies {
    implementation(Libs.materialDesign)
    implementation(Libs.Firebase.Crashlytics.crashlytics)
    implementation(Libs.pspdfkit)
    implementation(Libs.gson)
    implementation(Libs.ExoPlayer.exoPlayer)
    implementation(Libs.ExoPlayer.mediaUi)
    implementation(Libs.Coil.compose)
    implementation(Libs.Commons.io)
    implementation(Libs.Commons.codec)
    implementation(Libs.Commons.validator)
    implementation(Libs.timber)
    implementation(Libs.jodaTime)
    implementation(Libs.eventBus)
    implementation(Libs.keyboardVisibility)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
}

kapt {
    correctErrorTypes = true
}

fun gitLastCommitHash(): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --verify --short HEAD".split(" ")
            standardOutput = byteOut
        }
        String(byteOut.toByteArray()).trim().takeIf { it.isNotBlank() } ?: "Unknown Branch"
    } catch (e: Exception) {
        logger.warn("Unable to determine current branch: ${e.message}")
        "Unknown Branch"
    }
}

fun readPspdfkitKey(): String {
    val file = rootProject.file("pspdfkit-key.txt")
    if (!file.exists()) {
        logger.warn("pspdfkit-key.txt file not found. Using PSPDFKit without a key")
        return "\"\""
    }
    return "\"${file.readLines().first()}\""
}
