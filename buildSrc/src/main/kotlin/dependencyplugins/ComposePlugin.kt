package dependencyplugins

import Libs
import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType<TestedExtension>()
        extension.configure(project)
    }
}

private fun TestedExtension.configure(project: Project) {
    buildFeatures.apply {
        compose = true
    }

    project.dependencies.apply {
        add("implementation", Libs.Compose.ui)
        add("implementation", Libs.Compose.uiUtil)
        add("implementation", Libs.Compose.uiTooling)
        add("implementation", Libs.Compose.foundation)
        add("implementation", Libs.Compose.activity)
        add("implementation", Libs.Compose.liveData)
        add("implementation", Libs.Compose.navigation)
        add("implementation", Libs.Compose.constraintLayout)
        add("implementation", Libs.Compose.viewBinding)
        add("implementation", Libs.Compose.material3)
        add("implementation", Libs.Compose.material3WindowSize)
    }
}
