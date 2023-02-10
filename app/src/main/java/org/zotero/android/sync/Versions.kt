package org.zotero.android.sync

import org.zotero.android.database.objects.RVersions

class Versions(
    var collections: Int,
    var items: Int,
    var trash: Int,
    var searches: Int,
    var deletions: Int,
    var settings: Int
) {


    val max: Int get() {
        return Math.max(
            collections,
            Math.max(
                items,
                Math.max(
                    trash,
                    Math.max(
                        searches,
                        Math.max(deletions, settings)
                    )
                )
            )
        )
    }

    companion object {
        fun init(versions: RVersions?): Versions {
            return Versions(
                collections = versions?.collections ?: 0,
                items = versions?.items ?: 0,
                trash = versions?.trash ?: 0,
                searches = versions?.searches ?: 0,
                deletions = versions?.deletions ?: 0,
                settings = versions?.settings ?: 0,
            )
        }

        val empty: Versions
            get() {
                return Versions(0, 0, 0, 0, 0, 0)
            }
    }



}
