package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.PageIndexResponse
import org.zotero.android.api.pojo.sync.SettingsResponse
import org.zotero.android.api.pojo.sync.TagColorResponse
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier

class StoreSettingsDbRequest(
    private val response: SettingsResponse,
    private val libraryId: LibraryIdentifier,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val response = this.response.tagColors
        if (response != null) {
            syncTagColors(tags = response.value, database)
        }
        syncPages(this.response.pageIndices.indices, database)
    }

    private fun syncPages(pages: List<PageIndexResponse>, database: Realm) {
        when (this.libraryId) {
            is LibraryIdentifier.group -> {
                return
            }

            is LibraryIdentifier.custom -> {
                //no-op
            }
        }

        val indices = database.where<RPageIndex>().library(this.libraryId)

        for (index in pages) {
            val rIndex: RPageIndex
            val existing = indices.key(index.key).findFirst()
            if (existing != null) {
                rIndex = existing
            } else {
                rIndex = database.createObject<RPageIndex>()
                rIndex.key = index.key
                rIndex.libraryId = index.libraryId
            }
            rIndex.index = index.value
            rIndex.version = index.version

            rIndex.deleteAllChanges(database)
        }
    }

    private fun syncTagColors(tags: List<TagColorResponse>, database: Realm) {
        val names = tags.map { it.name }
        val toDelete = database.where<RTag>().library(this.libraryId)
            .isNotEmpty("color")
            .and()
            .not().`in`("name", names.toTypedArray())
            .findAll()
        for (tag in toDelete) {
            tag.tags?.deleteAllFromRealm()
        }
        toDelete.deleteAllFromRealm()

        val allTags = database.where<RTag>()
        for ((idx, tag) in tags.withIndex()) {
            val existing = allTags.name(tag.name, this.libraryId).findFirst()
            if (existing != null) {
                var didChange = false
                if (existing.color != tag.color) {
                    existing.color = tag.color
                    didChange = true
                }
                if (existing.order != idx) {
                    existing.order = idx
                    didChange = true
                }

                if (didChange) {
                    for (tag in existing.tags!!) {
                        val item = tag.item ?: continue
                        // Update item so that items list and tag picker are updated with color/order changes
                        item.rawType = item.rawType
                    }
                }
            } else {
                val new = database.createObject<RTag>()
                new.name = tag.name
                new.updateSortName()
                new.order = idx
                new.color = tag.color
                new.libraryId = this.libraryId
            }
        }

    }
}