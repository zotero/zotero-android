repositories {
    mavenCentral()
    google()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    // This constant is duplicated in root/build.gradle.kts. Make sure to also update there
    implementation("com.android.tools.build:gradle:8.12.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
}

gradlePlugin {
    plugins {
        register("ComposePlugin") {
            id = "composeDependencies"
            implementationClass = "dependencyplugins.ComposePlugin"
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