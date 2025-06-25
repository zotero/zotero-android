package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle

class InstallStyleDbRequest(private val identifier: String) : DbResponseRequest<Boolean> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): Boolean {
        val existing = database.where<RStyle>().equalTo("identifier", this.identifier).findFirst()
            ?: return false
        existing.installed = true
        return true
    }
}