package dependencyplugins

import Libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class AccompanistPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.Accompanist.systemuicontroller)
    }
}
