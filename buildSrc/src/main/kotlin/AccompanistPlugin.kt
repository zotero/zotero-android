import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AccompanistPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType<TestedExtension>()
        extension.configure(project)
    }
}

private fun TestedExtension.configure(project: Project) {
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
