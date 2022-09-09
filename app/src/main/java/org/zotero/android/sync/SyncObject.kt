package org.zotero.android.sync

enum class SyncObject {
    collection, search, item, trash, settings;

    val apiPath: String
        get() {
            return when (this) {
                collection ->
                    "collections"
                search ->
                    "searches"
                item ->
                    "items"
                trash ->
                    "items/trash"
                settings ->
                    "settings"
            }
        }
}