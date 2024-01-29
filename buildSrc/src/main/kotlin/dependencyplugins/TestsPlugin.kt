package dependencyplugins

import Libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class TestsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("testImplementation", Libs.Test.junit)
        add("testImplementation", Libs.Test.mockitoCore)
        add("testImplementation", Libs.Test.mockitoInline)
        add("testImplementation", Libs.Test.mockk)
        add("testImplementation", Libs.Test.kluentAndroid)

        add("testImplementation", Libs.Kotlin.Coroutines.test)

        add("testImplementation", Libs.AndroidX.Test.testCore)
        add("testImplementation", Libs.AndroidX.Test.coreTesting)

        add("androidTestImplementation", Libs.AndroidX.Test.testRules)
        add("androidTestImplementation", Libs.AndroidX.Test.runner)
        add("androidTestImplementation", Libs.AndroidX.Test.junit)

    }
}
