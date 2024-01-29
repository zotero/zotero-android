package dependencyplugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidXPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configure(project)
    }
}

private fun configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.AndroidX.core)
        add("implementation", Libs.AndroidX.fragment)
        add("implementation", Libs.AndroidX.appCompat)
        add("implementation", Libs.AndroidX.preferencesKtx)
        add("implementation", Libs.AndroidX.vectorDrawable)
        add("implementation", Libs.AndroidX.activity)
        add("implementation", Libs.AndroidX.constraintLayout)
        add("implementation", Libs.AndroidX.constraintLayoutSolver)
        add("implementation", Libs.AndroidX.swipeRefreshLayout)

        add("implementation", Libs.AndroidX.Lifecycle.lifecycleRuntime)
        add("implementation", Libs.AndroidX.Lifecycle.commonJava8)
        add("implementation", Libs.AndroidX.Lifecycle.liveData)
        add("implementation", Libs.AndroidX.Lifecycle.process)

        add("implementation", Libs.AndroidX.Work.runtime)

    }
}