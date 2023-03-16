package org.zotero.android.sync

sealed class CollectionIdentifier {
    enum class CustomType {
        all, trash, publications, unfiled
    }

    data class collection(val key: String) : CollectionIdentifier()
    data class search(val key: String) : CollectionIdentifier()
    data class custom(val type: CustomType) : CollectionIdentifier()

    val id: String
        get() {
            return when (this) {
                is custom -> {
                    when (this.type) {
                        CustomType.all -> "all"
                        CustomType.publications -> "publications"
                        CustomType.trash -> "trash"
                        CustomType.unfiled -> "unfiled"
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
                        CustomType.trash -> true
                        CustomType.all, CustomType.publications, CustomType.unfiled -> false
                    }
                }
                else -> false
            }
        }

    val keyGet: String? get() {
        when (this) {
            is collection -> return this.key
            is search -> return this.key
            is custom -> return null
        }
    }
}
