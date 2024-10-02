repositories {
    mavenCentral()
    google()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    // This constant is duplicated in root/build.gradle.kts. Make sure to also update there
    implementation("com.android.tools.build:gradle:8.2.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    // Without this Hilts aggregate dependencies task is unable to run.
    implementation("com.squareup:javapoet:1.13.0")
}

gradlePlugin {
    plugins {
        register("ComposePlugin") {
            id = "composeDependencies"
            implementationClass = "dependencyplugins.ComposePlugin"
        }
        register("AccompanistPlugin") {
            id = "accompanistDependencies"
            implementationClass = "dependencyplugins.AccompanistPlugin"
        }
        register("DaggerAndHiltPlugin") {
            id = "daggerAndHiltDependencies"
            implementationClass = "dependencyplugins.DaggerAndHiltPlugin"
        }
        register("TestsPlugin") {
            id = "testDependencies"
            implementationClass = "dependencyplugins.TestsPlugin"
        }
        register("NetworkPlugin") {
            id = "networkDependencies"
            implementationClass = "dependencyplugins.NetworkPlugin"
        }
        register("KotlinPlugin") {
            id = "kotlinDependencies"
            implementationClass = "dependencyplugins.KotlinPlugin"
        }
        register("AndroidXPlugin") {
            id = "androidXDependencies"
            implementationClass = "dependencyplugins.AndroidXPlugin"
        }
    }
}