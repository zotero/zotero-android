package org.zotero.android.sync

sealed class CollectionIdentifier {
    enum class CustomType {
        all, trash, publications, unfiled
    }

    data class collection(val key: String) : CollectionIdentifier()
    data class search(val key: String) : CollectionIdentifier()
    data class custom(val type: CustomType) : CollectionIdentifier()

    val id: String get() {
        when(this) {
            is CollectionIdentifier.custom -> {
                when (this.type) {
                    CustomType.all -> return "all"
                    CustomType.publications -> return "publications"
                    CustomType.trash -> return "trash"
                    CustomType.unfiled -> return "unfiled"
                }
            }
            is CollectionIdentifier.collection ->
            return "c_" + this.key
            is CollectionIdentifier.search ->
            return "s_" + this.key
        }
    }

    val isTrash: Boolean get() {
        when (this) {
            is CollectionIdentifier.custom -> {
                when (this.type) {
                    CustomType.trash -> return true
                    CustomType.all, CustomType.publications, CustomType.unfiled -> return false
                }
            }
            else -> return false
        }
    }
}
