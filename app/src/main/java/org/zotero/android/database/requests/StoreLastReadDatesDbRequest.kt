package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RLastReadDate
import org.zotero.android.database.objects.RLastReadDateChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.ktx.uniqueObject
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class StoreLastReadDatesDbRequest(private val array: List<Data>) : DbRequest {
    data class Data(
        val key: String,
        val libraryId: LibraryIdentifier,
        val date: Date?
    )


    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        array.forEach {
            StoreLastReadDateDbRequest(
                key = it.key,
                libraryId = it.libraryId,
                date = it.date
            ).process(database)
        }
    }
}

class StoreLastReadDateDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val date: Date?,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().findAll().uniqueObject(key = key, libraryId = libraryId)
            ?: return
        if (item.lastRead == date) {
            return
        }
        item.lastRead = date
        item.updateEffectiveLastRead()

        when (libraryId) {
            is LibraryIdentifier.custom -> {
                when (libraryId.type) {
                    RCustomLibraryType.myLibrary -> {
                        handleMyLibraryDate(item)
                    }
                }
            }

            is LibraryIdentifier.group -> {
                handleGroupDate(database)
            }

        }
    }

    fun handleMyLibraryDate(item: RItem) {
        item.changes.add(
            RObjectChange.create(
                changes = listOf(
                    RItemChanges.lastRead
                )
            )
        )

        item.changeType = UpdatableChangeType.user.name
    }

    fun handleGroupDate(database: Realm) {
        val lastReadDate: RLastReadDate
        val existing =
            database.where<RLastReadDate>().findAll().uniqueObject(key = key, libraryId = libraryId)
        if (existing != null) {
            if (existing.date == date) {
                return
            }
            lastReadDate = existing
        } else {
            lastReadDate = database.createObject<RLastReadDate>()
            lastReadDate.key = key
            lastReadDate.libraryId = libraryId
        }

        if (date != null) {
            lastReadDate.date = date
            lastReadDate.changes.add(
                RObjectChange.create(
                    changes = listOf(
                        RLastReadDateChanges.date
                    )
                )
            )
        } else {
            lastReadDate.deleted = true
        }
        lastReadDate.changeType = UpdatableChangeType.user.name
    }
}
