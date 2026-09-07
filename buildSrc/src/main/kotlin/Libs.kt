object Libs {

    const val androidJUnitRunner = "androidx.test.runner.AndroidJUnitRunner"

    const val nutrient = "com.pspdfkit:pspdfkit:2024.4.0"
    const val googleServices = "com.google.gms:google-services:4.5.0"
    const val realmGradlePlugin = "io.realm:realm-gradle-plugin:10.19.0"
    const val materialDesign = "com.google.android.material:material:1.14.0"
    const val gson = "com.google.code.gson:gson:2.14.0"
    const val timber = "com.jakewharton.timber:timber:5.0.1"
    const val jodaTime = "joda-time:joda-time:2.14.3"
    const val eventBus = "org.greenrobot:eventbus:3.3.1"
    const val keyboardVisibility =
        "net.yslibrary.keyboardvisibilityevent:keyboardvisibilityevent:3.0.0-RC3"

    const val codeScanner = "com.google.android.gms:play-services-code-scanner:16.1.0"

    object Compose {
        private const val version = "1.11.4"
        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val liveData = "androidx.compose.runtime:runtime-livedata:$version"
        const val ui = "androidx.compose.ui:ui:$version"
        const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
        const val uiUtil = "androidx.compose.ui:ui-util:$version"
        const val viewBinding = "androidx.compose.ui:ui-viewbinding:$version"
        const val activity = "androidx.activity:activity-compose:1.13.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.1.1"
        const val navigation = "androidx.navigation:navigation-compose:2.9.8"

        const val material3 = "androidx.compose.material3:material3:1.5.0-alpha02"
        const val material3WindowSize =
            "androidx.compose.material3:material3-window-size-class:1.5.0-alpha24"
    }


    object Test {
        const val junit = "junit:junit:4.13.2"
        const val mockitoCore = "org.mockito:mockito-core:5.18.0"
        const val mockk = "io.mockk:mockk:1.14.5"
        const val kluentAndroid = "org.amshove.kluent:kluent-android:1.73"
    }

    object Hilt {
        const val version = "2.60.1"
        const val hiltAndroid = "com.google.dagger:hilt-android:$version"
        const val hiltAndroidCompiler = "com.google.dagger:hilt-android-compiler:$version"

        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:1.4.0"
    }

    object Coil {
        private const val version = "3.5.0"
        const val compose = "io.coil-kt.coil3:coil-compose:$version"
    }

    object Retrofit {
        private const val version = "3.0.0"
        const val core = "com.squareup.retrofit2:retrofit:$version"
        const val converterGson = "com.squareup.retrofit2:converter-gson:$version"
        const val converterScalars = "com.squareup.retrofit2:converter-scalars:$version"

        const val digest = "io.github.rburgst:okhttp-digest:3.1.1"
        const val kotlinSerialization =
            "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0"
    }

    object OkHttp {
        private const val version = "5.4.0"
        const val core = "com.squareup.okhttp3:okhttp:$version"
        const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object ExoPlayer {
        private const val version = "1.10.1"
        const val exoPlayer = "androidx.media3:media3-exoplayer:$version"
        const val mediaUi = "androidx.media3:media3-ui:$version"
    }

    object Firebase {

        object Crashlytics {
            const val crashlyticsGradle = "com.google.firebase:firebase-crashlytics-gradle:3.0.7"
            const val crashlytics = "com.google.firebase:firebase-crashlytics-ktx:19.4.4"
        }
    }

    object Kotlin {
        const val version = "2.4.10" //Also change in settings.gradle.kts
//        const val serialization = "org.jetbrains.kotlin:kotlin-serialization:$version"
//        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"

        const val kspVersion = "2.3.10"

        const val jsonSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0"
        const val immutableCollections = "org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.1"

        object Coroutines {
            private const val version = "1.11.0"
            const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object AndroidX {
        const val activity = "androidx.activity:activity-ktx:1.13.0"
        const val appCompat = "androidx.appcompat:appcompat:1.7.1"

        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.2.1"
        const val constraintLayoutSolver = "androidx.constraintlayout:constraintlayout-solver:2.0.4"
        const val core = "androidx.core:core-ktx:1.19.0"

        const val fragment = "androidx.fragment:fragment-ktx:1.8.9"

        object Lifecycle {
            private const val version = "2.11.0"
            const val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val commonJava8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val process = "androidx.lifecycle:lifecycle-process:$version"
            const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val lifecycle = "androidx.lifecycle:lifecycle-viewmodel-compose:$version"
        }

        const val vectorDrawable = "androidx.vectordrawable:vectordrawable:1.2.0"
        const val preferencesKtx = "androidx.preference:preference-ktx:1.2.1"

        object Test {
            const val junit = "androidx.test.ext:junit:1.3.0"
            const val runner = "androidx.test:runner:1.7.0"
            const val testRules = "androidx.test:rules:1.7.0"
            const val testCore = "androidx.test:core:1.7.0"
            const val coreTesting = "androidx.arch.core:core-testing:2.2.0"
        }

    }
    object Commons {
        const val text = "org.apache.commons:commons-text:1.15.0"
    }
}
