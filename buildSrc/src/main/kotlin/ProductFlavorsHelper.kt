import org.gradle.api.NamedDomainObjectContainer

fun com.android.build.gradle.TestedExtension.setDefaultProductFlavors() {
    flavorDimensions(ProductFlavors.Environment.dimension)

    productFlavors {
        create(ProductFlavors.Environment.DEV.envName()) {
            dimension = ProductFlavors.Environment.dimension
            create(ProductFlavors.Environment.STAGING.envName()) {
                dimension = ProductFlavors.Environment.dimension
            }
        }
        create(ProductFlavors.Environment.PROD.envName()) {
            dimension = ProductFlavors.Environment.dimension
        }
    }
}

fun <T> NamedDomainObjectContainer<T>.dev(block: T.() -> Unit) {
    getByName(ProductFlavors.Environment.DEV.envName()).block()
}

fun <T> NamedDomainObjectContainer<T>.staging(block: T.() -> Unit) {
    getByName(ProductFlavors.Environment.STAGING.envName()).block()
}

fun <T> NamedDomainObjectContainer<T>.prod(block: T.() -> Unit) {
    getByName(ProductFlavors.Environment.PROD.envName()).block()
}

private val ignoreSet = setOf("devRelease", "stagingRelease")

//fun VariantBuilder.ignoreUnusedVariants() {
//    this.enabled = !ignoreSet.contains(name)
//}
