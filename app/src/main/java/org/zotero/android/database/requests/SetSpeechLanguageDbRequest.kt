package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier

class SetSpeechLanguageDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
    val language: String?,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database
            .where<RItem>()
            .key(key, libraryId)
            .findFirst() ?: return
        if (item.speechLanguage == language) {
            return
        }
        item.speechLanguage = language
    }
}