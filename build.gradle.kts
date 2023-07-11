import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.21")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.6")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.43.2")
        classpath("io.realm:realm-gradle-plugin:10.16.1")
        classpath("com.google.gms:google-services:4.3.15")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven {
            url = uri("https://customers.pspdfkit.com/maven")
        }
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            suppressWarnings = true
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xopt-in=androidx.paging.ExperimentalPagingApi",
                "-Xopt-in=kotlinx.coroutines.FlowPreview",
                "-Xopt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi",
                "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-Xopt-in=androidx.compose.ui.text.ExperimentalTextApi",
                "-Xopt-in=androidx.compose.animation.ExperimentalAnimationApi",
                "-Xopt-in=com.google.accompanist.pager.ExperimentalPagerApi",
                "-Xopt-in=coil.annotation.ExperimentalCoilApi",
                "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi",
                "-Xopt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
                "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            )
        }
    }
}
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
