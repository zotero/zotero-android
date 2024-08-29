package dependencyplugins

import Libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class NetworkPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.Retrofit.kotlinSerialization)
        add("implementation", Libs.Retrofit.core)
        add("implementation", Libs.Retrofit.converterGson)
        add("implementation", Libs.Retrofit.converterScalars)
        add("implementation", Libs.Retrofit.digest)

        add("implementation", Libs.OkHttp.core)
        add("implementation", Libs.OkHttp.loggingInterceptor)
    }
}
