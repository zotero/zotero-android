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
        add("implementation", Libs.Dagger.hiltAndroid)
        add("kapt", Libs.Dagger.hiltCompiler)

        add("kapt", Libs.Dagger.hiltAndroidAndroidCompilerProcessor)

        add("kapt", Libs.Hilt.compiler)
        add("implementation", Libs.Hilt.navigationCompose)

    }
}
