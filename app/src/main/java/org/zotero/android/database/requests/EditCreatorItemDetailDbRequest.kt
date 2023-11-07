package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.sync.LibraryIdentifier

class EditCreatorItemDetailDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val creator: ItemDetailCreator,
    private val orderId: Int,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(key, libraryId).findFirst() ?: return

        val rCreator: RCreator
        val _creator = item.creators.filter {it.uuid == creator.id.toString()}.firstOrNull()

        if (_creator != null) {
            rCreator = _creator
        } else {
            rCreator = database.createEmbeddedObject(RCreator::class.java, item, "creators")
            rCreator.uuid = creator.id.toString()
        }

        rCreator.rawType = creator.type
        rCreator.orderId = orderId
        rCreator.primary = creator.primary

        when (creator.namePresentation) {
            ItemDetailCreator.NamePresentation.full -> {
                rCreator.name = creator.fullName
                rCreator.firstName = ""
                rCreator.lastName = ""
            }
            ItemDetailCreator.NamePresentation.separate -> {
                rCreator.name = ""
                rCreator.firstName = creator.firstName
                rCreator.lastName = creator.lastName
            }
        }

        item.updateCreatorSummary()
        item.changes.add(RObjectChange.create(listOf(RItemChanges.creators)))
    }
}