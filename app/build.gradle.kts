plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("dagger.hilt.android.plugin")
    id("realm-android")
    id("kotlin-parcelize")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "org.zotero.android"
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "true")
        buildConfigField ("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "false")
//        manifestPlaceholders([enableCrashReporting: "false"])
        buildConfigField ("String", "BASE_API_URL", "\"https://api.zotero.org\"")
                manifestPlaceholders["enableCrashReporting"] =  false
    }
    signingConfigs {
        create("release") {
//            storeFile = rootProject.file("release.keystore")

            //To be replaced before public release
            storeFile = file("zotero.temp.keystore")
            keyAlias = "zotero.temp.keystore"
            storePassword =  "0uilyM8v9iPi"
            keyPassword = "0uilyM8v9iPi"
        }
    }
    androidResources {
        noCompress ("ttf", "mov", "avi", "json", "html", "csv", "obb")
    }
    buildTypes {
//        getByName("internalQAForDevug") {
//            isDebuggable = true
//            isMinifyEnabled = false
//            resValue ("string", "application_name", "Zotero Dev")
//            applicationIdSuffix = ".internaldev"
//            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            signingConfigs
//                .findByName("debug")
//                ?.storeFile = rootProject.file("debug.keystore")
//        manifestPlaceholders["enableCrashReporting"] =  false
//        }
//
//        getByName("internalQA") {
//            isDebuggable = false
//            isMinifyEnabled = true
//            resValue ("string", "application_name", "Zotero QA")
//            applicationIdSuffix = ".internalqa"
//            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            signingConfigs
//                .findByName("debug")
//                ?.storeFile = rootProject.file("debug.keystore")
//            buildConfigField ("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
        //        manifestPlaceholders["enableCrashReporting"] =  true
//
//        }
//
//        getByName("earlyAccess") {
//            isDebuggable = false
//            isMinifyEnabled = true
//            resValue ("string", "application_name", "Zotero Preview")
//            applicationIdSuffix = ".internalqa"
//            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            signingConfigs
//                .findByName("debug")
//                ?.storeFile = rootProject.file("debug.keystore")
//            buildConfigField ("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
//            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "false")
        //        manifestPlaceholders["enableCrashReporting"] =  true
//
//        }
//
//        getByName("release") {
//            isDebuggable = false
//            isMinifyEnabled = true
//            resValue("string", "application_name", "Zotero")
//
//            applicationIdSuffix = ".internalqa"
//            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            signingConfigs
//                .findByName("debug")
//                ?.storeFile = rootProject.file("debug.keystore")
//            buildConfigField ("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
//
//            buildConfigField ("boolean", "EVENT_AND_CRASH_LOGGING_ENABLED", "true")
////            manifestPlaceholders = [enableCrashReporting: "true"]
//
//            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "false")
        //        manifestPlaceholders["enableCrashReporting"] =  true
//
//        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0-beta02"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
//    packagingOptions {
//        exclude('META-INF/rxkotlin.properties')
//        exclude('META-INF/rxkotlin_main.kotlin_module')
//    }
//    packagingOptions {
//        exclude('META-INF/DEPENDENCIES')
//        exclude('META-INF/LICENSE')
//        exclude('META-INF/LICENSE.txt')
//        exclude( 'META-INF/license.txt')
//        exclude( 'META-INF/NOTICE')
//        exclude('META-INF/NOTICE.txt')
//        exclude('META-INF/notice.txt')
//        exclude('META-INF/ASL2.0')
//        exclude("META-INF/*.kotlin_module")
//        exclude( 'META-INF/io.netty.versions.properties')
//        exclude('META-INF/INDEX.LIST')
//        exclude( 'META-INF/rxjava.properties')
//        exclude( 'META-INF/native-image/io.netty/transport/reflection-config.json')
//        pickFirst = 'META-INF/native-image/io.netty/**/native-image.properties'
//    }
    
}

dependencies {

    //AndroidX
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.1")
    implementation("androidx.lifecycle:lifecycle-process:2.4.1")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.constraintlayout:constraintlayout-solver:2.0.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.google.android.material:material:1.5.0")

    //Compose
    implementation("androidx.compose.ui:ui-viewbinding:1.2.0-beta02")
    implementation("androidx.compose.ui:ui-util:1.2.0-beta02")
    implementation("androidx.compose.ui:ui-tooling:1.2.0-beta02")
    implementation("androidx.compose.ui:ui:1.2.0-beta02")
    implementation("androidx.navigation:navigation-compose:2.5.0-rc01")
    implementation("androidx.compose.material:material:1.2.0-beta02")
    implementation("androidx.compose.runtime:runtime-livedata:1.2.0-beta02")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.0-rc01")
    implementation("androidx.compose.foundation:foundation:1.2.0-beta02")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0")
    implementation("androidx.activity:activity-compose:1.5.0-rc01")

    //Accompanist
    implementation("com.google.accompanist:accompanist-insets:0.23.1")
    implementation("com.google.accompanist:accompanist-placeholder:0.23.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.23.1")
    implementation("com.google.accompanist:accompanist-pager:0.23.1")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.23.1")


    //Glide
    implementation("com.github.bumptech.glide:glide:4.11.0")
    kapt ("com.github.bumptech.glide:compiler:4.11.0")

    //Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.21")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")

    //Dagger + Hilt
    implementation("com.google.dagger:hilt-android:2.43.2")
    implementation("com.google.dagger:dagger:2.43.2")
    kapt("com.google.dagger:dagger-compiler:2.43.2")
    kapt("com.google.dagger:hilt-compiler:2.43.2")

    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    //Crash & Analytics
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("com.google.android.play:core-ktx:1.8.1")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.2.12")
    implementation("com.google.firebase:firebase-analytics-ktx:21.1.0")

    //Other
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("joda-time:joda-time:2.10.9")
    implementation("com.google.guava:guava:25.0-android")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    implementation("org.greenrobot:eventbus:3.2.0")
    implementation("com.airbnb.android:lottie:5.0.3")

    implementation("commons-io:commons-io:2.6")
    implementation ("commons-codec:commons-codec:1.13")


    implementation("androidx.work:work-runtime-ktx:2.7.1")

    //Retrofit 2
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    //GSON
    implementation("com.google.code.gson:gson:2.8.8")

    //Ok HTTP
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    //Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")

    testImplementation("org.mockito:mockito-core:3.6.0")
    testImplementation("org.mockito:mockito-inline:3.11.0")

    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation("org.amshove.kluent:kluent-android:1.61")

    //Instrumented Tests
    androidTestImplementation("androidx.test:rules:1.3.0")
    androidTestImplementation("androidx.test:runner:1.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")

    
}
