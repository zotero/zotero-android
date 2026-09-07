package org.zotero.android.sync

import kotlinx.serialization.Serializable

@Serializable
sealed class CollectionIdentifier: java.io.Serializable {
    @Serializable
    enum class CustomType: java.io.Serializable {
        all, trash, publications, unfiled, recentlyRead
    }

    @Serializable
    data class collection(val key: String) : CollectionIdentifier()
    @Serializable
    data class search(val key: String) : CollectionIdentifier()
    @Serializable
    data class custom(val type: CustomType) : CollectionIdentifier()

    val id: String
        get() {
            return when (this) {
                is custom -> {
                    when (this.type) {
                        CustomType.all -> {
                            "all"
                        }
                        CustomType.publications -> {
                            "publications"
                        }
                        CustomType.trash -> {
                            "trash"
                        }
                        CustomType.unfiled -> {
                            "unfiled"
                        }
                        CustomType.recentlyRead -> {
                            "recentlyRead"
                        }
                    }
                }
                is collection ->
                    "c_" + this.key
                is search ->
                    "s_" + this.key
            }
        }

    val isTrash: Boolean
        get() {
            return when (this) {
                is custom -> {
                    when (this.type) {
                        CustomType.trash -> {
                            true
                        }
                        CustomType.all, CustomType.publications, CustomType.unfiled, CustomType.recentlyRead -> {
                            false
                        }
                    }
                }
                else -> false
            }
        }

    val keyGet: String? get() {
        return when (this) {
            is collection -> this.key
            is search -> this.key
            is custom -> null
        }
    }

    val isCollection: Boolean get() {
        return when (this) {
            is collection -> true
            else -> false
        }
    }
}
