package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.LastReadResponse
import org.zotero.android.api.pojo.sync.PageIndexResponse
import org.zotero.android.api.pojo.sync.SettingsResponse
import org.zotero.android.api.pojo.sync.TagColorResponse
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RLastReadDate
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.ktx.uniqueObject
import org.zotero.android.ktx.uniqueObjectV2
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

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
        when(this.libraryId) {
            is LibraryIdentifier.group -> {
                return
            }
            is LibraryIdentifier.custom -> {
                //no-op
            }
        }

        syncPages(pages = this.response.pageIndices.indices, database)
        syncLastReadValues(values=  this.response.lastReadValues.values, database)
    }

    private fun syncPages(pages: List<PageIndexResponse>, database: Realm) {
        for (index in pages) {
            val rIndex: RPageIndex
            val existing = database
                .where<RPageIndex>()
                .uniqueObjectV2(key = index.key, libraryId = index.libraryId)
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
        val toDelete = database.where<RTag>()
            .library(this.libraryId)
            .and()
            .rawPredicate("color != \"\"")
            .and()
            .not().`in`("name", names.toTypedArray())
            .findAll()
        for (tag in toDelete) {
            tag.tags?.deleteAllFromRealm()
        }
        toDelete.deleteAllFromRealm()

        val allTags = database.where<RTag>().findAll()
        for ((idx, tag) in tags.withIndex()) {
            val existing = allTags.where().name(tag.name, this.libraryId).findFirst()
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

    private fun syncLastReadValues(values: List<LastReadResponse>, database: Realm) {
        for (value in values) {
            val rDate: RLastReadDate
            val existing = database.where<RLastReadDate>().findAll()
                .uniqueObject(key = value.key, libraryId = value.libraryId)
            if (existing != null) {
                rDate = existing
            } else {
                rDate = database.createObject<RLastReadDate>()
                rDate.key = value.key
                rDate.libraryId = value.libraryId
            }
            rDate.date = Date(value.value * 1000)
            rDate.version = value.version

            rDate.deleteAllChanges(database)

            val item = database.where<RItem>().findAll()
                .uniqueObject(key = value.key, libraryId = value.libraryId)

            if (item != null) {
                item.lastRead = rDate.date
                item.updateEffectiveLastRead()
            } else {
                val item = database.createObject<RItem>()
                item.key = value.key
                item.libraryId = value.libraryId
                item.lastRead = rDate.date
                item.updateEffectiveLastRead()
                item.syncState = ObjectSyncState.dirty.name
                item.lastSyncDate = Date(0)
                item.changeType = UpdatableChangeType.sync.name
            }
        }
    }
}