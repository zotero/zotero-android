import java.util.Locale

object ProductFlavors {
    enum class Environment(
        val url: String,
    ) {
        DEV(
            url = "",
        ),
        INTERNAL(
            url = "",
        );

        companion object {
            const val dimension = "environment"
        }
    }
}

@Suppress("DefaultLocale")
fun ProductFlavors.Environment.envName() = name.lowercase(Locale.getDefault())
