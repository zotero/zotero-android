package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle

class UninstallStyleDbRequest(
    private val identifier: String
):DbResponseRequest<List<String>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): List<String> {
        val style = database.where<RStyle>().equalTo("identifier", this.identifier).findFirst() ?: return emptyList()

        // If some styles are dependent on this one, just flip the `installed` flag.
        if (!style.dependent!!.isEmpty()) {
            style.installed = false
            return emptyList()
        }

        val toRemove = mutableListOf(style.filename)
        val dependency = style.dependency
        if (dependency != null) {
            // If the dependency is not installed and it doesn't have any other depending styles, just delete it.
            if (!dependency.installed && dependency.dependent!!.size == 1) {
                toRemove.add(dependency.filename)
                dependency.deleteFromRealm()
            }
        }

        style.deleteFromRealm()
        return toRemove
    }
}