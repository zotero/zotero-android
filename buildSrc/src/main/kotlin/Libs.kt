object Libs {

    object Accompanist {
        private const val version = "0.31.5-beta"
        const val flowlayout = "com.google.accompanist:accompanist-flowlayout:$version"
        const val insets = "com.google.accompanist:accompanist-insets:$version"
        const val navigationAnimation =
            "com.google.accompanist:accompanist-navigation-animation:$version"
        const val pager = "com.google.accompanist:accompanist-pager:$version"
        const val placeholder = "com.google.accompanist:accompanist-placeholder:$version"
        const val swipeToRefresh = "com.google.accompanist:accompanist-swiperefresh:$version"
        const val systemuicontroller =
            "com.google.accompanist:accompanist-systemuicontroller:$version"
    }

    object Compose {
        const val compileVersion = "1.5.4"

        private const val version = "1.6.0-rc01"
        const val activity = "androidx.activity:activity-compose:1.8.1"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.0.1"
        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val lifecycle = "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2"
        const val liveData = "androidx.compose.runtime:runtime-livedata:$version"
        const val navigation = "androidx.navigation:navigation-compose:2.7.5"
        const val ui = "androidx.compose.ui:ui:$version"
        const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
        const val uiUtil = "androidx.compose.ui:ui-util:$version"
        const val viewBinding = "androidx.compose.ui:ui-viewbinding:$version"
        const val material = "androidx.compose.material:material:$version"
        const val material3 = "androidx.compose.material3:material3:1.1.2"
        const val material3WindowSize = "androidx.compose.material3:material3-window-size-class:1.1.2"
    }


    object Test {
        const val junit = "junit:junit:4.13.2"
        const val testCore = "androidx.test:core:1.5.0"
        const val coreTesting = "androidx.arch.core:core-testing:2.2.0"
        const val mockitoCore = "org.mockito:mockito-core:3.11.0"
        const val mockitoInline = "org.mockito:mockito-inline:3.11.0"
        const val mockk = "io.mockk:mockk:1.11.0"
        const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1"
        const val kluentAndroid = "org.amshove.kluent:kluent-android:1.72"

        const val testRules = "androidx.test:rules:1.5.0"
        const val testRunner = "androidx.test:runner:1.5.2"
        const val androidXJunit = "androidx.test.ext:junit:1.1.5"
    }

    object Dagger {
        private const val version = "2.48"
        const val hiltAndroid = "com.google.dagger:hilt-android:$version"
        const val hiltCompiler = "com.google.dagger:hilt-compiler:$version"
        const val hiltGradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val hiltAndroidAndroidCompilerProcessor = "com.google.dagger:hilt-android-compiler:$version"
    }

    object Hilt {
        private const val version = "1.1.0"

        const val worker = "androidx.hilt:hilt-work:$version"
        const val compiler = "androidx.hilt:hilt-compiler:$version"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:$version"
    }

    object Coil {
        private const val version = "2.2.2"
        const val compose = "io.coil-kt:coil-compose:$version"
    }
    object Retrofit {
        private const val version = "2.9.0"
        const val core = "com.squareup.retrofit2:retrofit:$version"
        const val converterGson = "com.squareup.retrofit2:converter-gson:$version"
        const val converterScalars = "com.squareup.retrofit2:converter-scalars:$version"

        const val kotlinSerialization = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0"
    }
}