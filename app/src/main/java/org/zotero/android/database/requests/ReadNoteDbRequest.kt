package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.Note

class ReadNoteDbRequest(
    val key: String,
) : DbResponseRequest<Note?> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Note? {
        val item = database
            .where<RItem>()
            .key(key)
            .findFirst()
        return item?.let { Note.init(it) }
    }

}