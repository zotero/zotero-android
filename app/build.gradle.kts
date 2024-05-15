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

    //----Dependency plugins start----
    id("kotlinDependencies")
    id("androidXDependencies")
    id("composeDependencies")
    id("accompanistDependencies")
    id("daggerAndHiltDependencies")
    id("networkDependencies")
    id("testDependencies")
    //----Dependency plugins end----

    id("com.google.dagger.hilt.android")
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
                val secrets: List<String> = rootProject
                    .file("keystore-secrets.txt")
                    .readLines()
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
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfigs
                .findByName("debug")
                ?.storeFile = rootProject.file("debug.keystore")

            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
            manifestPlaceholders["enableCrashReporting"] = false
            extra.set("enableCrashlytics", false)
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getAt("release")

            buildConfigField("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
            manifestPlaceholders["enableCrashReporting"] = true
        }
    }
    setDefaultProductFlavors()
    productFlavors {
        dev {
            resValue("string", "app_name", """"Zotero Debug""")
            buildConfigField("String", "PSPDFKIT_KEY", readPspdfkitKey())
            applicationIdSuffix = ".debug"
        }
        internal {
            resValue("string", "app_name", """"Zotero Beta""")
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
        beforeVariants { it.ignoreUnusedVariants() }
//        onVariants {
//            it.outputs.forEach { output ->
//                val newVersionName = "${BuildConfig.version.name}-${it.flavorName}.${gitLastCommitHash()}"
//                output.versionName.set(newVersionName)
//            }
//        }
    }
}

play {
    track.set("internal")
    defaultToAppBundles.set(true)
    resolutionStrategy.set(ResolutionStrategy.AUTO)
}

dependencies {

    //Material design
    implementation(Libs.materialDesign)

    //Crash
    implementation(Libs.Firebase.Crashlytics.crashlytics)

    //PSPDFKIT
    implementation(Libs.pspdfkit)

    //GSON
    implementation(Libs.gson)

    //ExoPlayer
    implementation(Libs.ExoPlayer.exoPlayer)
    implementation(Libs.ExoPlayer.mediaUi)

    //Coil
    implementation(Libs.Coil.compose)

    //Commons
    implementation(Libs.Commons.io)
    implementation(Libs.Commons.codec)
    implementation(Libs.Commons.validator)

    //Other
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
        String(byteOut.toByteArray()).trim().also {
            if (it == "HEAD")
                logger.warn("Unable to determine current branch: Project is checked out with detached head!")
        }
    } catch (e: Exception) {
        logger.warn("Unable to determine current branch: ${e.message}")
        "Unknown Branch"
    }
}

fun readPspdfkitKey() : String {
    val file = rootProject
        .file("pspdfkit-key.txt")
    if (!file.exists()) {
        logger.warn("pspdfkit-key.txt file not found. Using PSPDFKit without a key")
        return "\"\""
    }
    val keys: List<String> = file
        .readLines()
    return "\"${keys[0]}\""
}