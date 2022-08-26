object ProductFlavors {
    enum class Environment(
        val url: String,
    ) {
        DEV(
            url = "",
        ),
        STAGING(
            url = "",
        ),
        PROD(
            url = "",
        );

        companion object {
            const val dimension = "environment"
        }
    }
}

@Suppress("DefaultLocale")
fun ProductFlavors.Environment.envName() = name.toLowerCase()
