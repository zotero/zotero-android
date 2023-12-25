import com.android.build.api.variant.VariantBuilder
import org.gradle.api.NamedDomainObjectContainer

fun com.android.build.gradle.TestedExtension.setDefaultProductFlavors() {
    flavorDimensions(ProductFlavors.Environment.dimension)

    productFlavors {
        create(ProductFlavors.Environment.DEV.envName()) {
            dimension = ProductFlavors.Environment.dimension
            create(ProductFlavors.Environment.INTERNAL.envName()) {
                dimension = ProductFlavors.Environment.dimension
            }
        }
    }
}

fun <T> NamedDomainObjectContainer<T>.dev(block: T.() -> Unit) {
    getByName(ProductFlavors.Environment.DEV.envName()).block()
}

fun <T> NamedDomainObjectContainer<T>.internal(block: T.() -> Unit) {
    getByName(ProductFlavors.Environment.INTERNAL.envName()).block()
}

private val ignoreSet = setOf("devRelease", "internalDebug", "betaDebug")

fun VariantBuilder.ignoreUnusedVariants() {
    this.enable = !ignoreSet.contains(name)
}
