package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle
import timber.log.Timber

class ReadStyleDbRequest(private val identifier: String) : DbResponseRequest<RStyle> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RStyle {
        Timber.d("ReadStyleDbRequest: trying to read style with identifier: ${identifier}")
        val existing: RStyle? =
            database.where<RStyle>().equalTo("identifier", this.identifier).findFirst()
        if (existing == null) {
            throw DbError.objectNotFound
        }
        Timber.d("ReadStyleDbRequest: style with identifier found: ${identifier}, class = ${existing.javaClass}")
        return existing
    }
}