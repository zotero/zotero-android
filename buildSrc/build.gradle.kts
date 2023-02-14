repositories {
    mavenCentral()
    google()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    // This constant is duplicated in root/build.gradle.kts. Make sure to also update there
    implementation("com.android.tools.build:gradle:7.4.1")
    // Without this dependency the compiler has problems with inline Composables
    // This constant is duplicated in buildSrc/src/main/kotlin/Libs. Make sure to also update there
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    // Without this Hilts aggregate dependencies task is unable to run.
    implementation("com.squareup:javapoet:1.13.0")
}

