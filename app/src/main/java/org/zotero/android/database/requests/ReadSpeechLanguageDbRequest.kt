package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier

class ReadSpeechLanguageDbRequest(
    val key: String,
    val libraryId: LibraryIdentifier,
) : DbResponseRequest<String> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): String {
        return database
            .where<RItem>()
            .key(key, libraryId)
            .findFirst()
            ?.speechLanguage
            .orEmpty()
    }
}