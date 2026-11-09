package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RLastReadAloudPosition
import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.sync.LibraryIdentifier

data class LastReadAloudPositionResult(val position: ReadAloudResumePosition?)

class ReadLastReadAloudPositionDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
) : DbResponseRequest<LastReadAloudPositionResult> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): LastReadAloudPositionResult {
        val rPosition = database.where<RLastReadAloudPosition>().key(attachmentKey, libraryId).findFirst()
        return LastReadAloudPositionResult(position = rPosition?.takeIf { !it.deleted }?.position)
    }
}