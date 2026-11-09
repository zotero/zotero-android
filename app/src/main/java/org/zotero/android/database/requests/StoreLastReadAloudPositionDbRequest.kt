package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RLastReadAloudPosition
import org.zotero.android.database.objects.RLastReadAloudPositionChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.sync.LibraryIdentifier

class StoreLastReadAloudPositionDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val position: ReadAloudResumePosition,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val rPosition: RLastReadAloudPosition
        val existing = database.where<RLastReadAloudPosition>().key(key, libraryId).findFirst()
        if (existing != null) {
            if (existing.matches(position)) {
                return
            }
            rPosition = existing
        } else {
            rPosition = database.createObject<RLastReadAloudPosition>()
            rPosition.key = key
            rPosition.libraryId = libraryId
        }

        rPosition.set(position, database)
        rPosition.deleted = false
        rPosition.changes.add(RObjectChange.create(listOf(RLastReadAloudPositionChanges.position)))
        rPosition.changeType = UpdatableChangeType.user.name
    }
}