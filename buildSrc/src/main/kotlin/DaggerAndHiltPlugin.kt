import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class DaggerAndHiltPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType<TestedExtension>()
        extension.configure(project)
    }
}

private fun TestedExtension.configure(project: Project) {
    project.dependencies.apply {
        add("implementation", Libs.Dagger.hiltAndroid)
        add("kapt", Libs.Dagger.hiltCompiler)

        add("annotationProcessor", Libs.Dagger.hiltAndroidAndroidCompilerProcessor)

        add("implementation", Libs.Hilt.worker)
        add("kapt", Libs.Hilt.compiler)
        add("implementation", Libs.Hilt.navigationCompose)

    }
}
