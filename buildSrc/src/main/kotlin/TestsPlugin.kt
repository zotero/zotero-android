import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class TestsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType<TestedExtension>()
        extension.configure(project)
    }
}

private fun TestedExtension.configure(project: Project) {
    project.dependencies.apply {
        add("testImplementation", Libs.Test.junit)
        add("testImplementation", Libs.Test.testCore)
        add("testImplementation", Libs.Test.coreTesting)
        add("testImplementation", Libs.Test.mockitoCore)
        add("testImplementation", Libs.Test.mockitoInline)
        add("testImplementation", Libs.Test.mockk)
        add("testImplementation", Libs.Test.kotlinCoroutinesTest)
        add("testImplementation", Libs.Test.kluentAndroid)

        add("androidTestImplementation", Libs.Test.testRules)
        add("androidTestImplementation", Libs.Test.testRunner)
        add("androidTestImplementation", Libs.Test.androidXJunit)

    }
}
