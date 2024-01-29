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
        add("implementation", Libs.Accompanist.flowlayout)
        add("implementation", Libs.Accompanist.insets)
        add("implementation", Libs.Accompanist.navigationAnimation)
        add("implementation", Libs.Accompanist.pager)
        add("implementation", Libs.Accompanist.placeholder)
        add("implementation", Libs.Accompanist.swipeToRefresh)
        add("implementation", Libs.Accompanist.systemuicontroller)
    }
}
