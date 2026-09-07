package dependencyplugins

import Libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class DaggerAndHiltPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.Hilt.hiltAndroid)
        add("ksp", Libs.Hilt.hiltAndroidCompiler)

        add("implementation", Libs.Hilt.navigationCompose)
    }
}
