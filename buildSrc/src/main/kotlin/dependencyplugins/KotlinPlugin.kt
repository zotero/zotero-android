package dependencyplugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.Kotlin.stdlib)
        add("implementation", Libs.Kotlin.reflect)
        add("implementation", Libs.Kotlin.serialization)
        add("implementation", Libs.Kotlin.jsonSerialization)
        add("implementation", Libs.Kotlin.immutableCollections)

        add("implementation", Libs.Kotlin.Coroutines.core)
        add("implementation", Libs.Kotlin.Coroutines.android)
    }
}