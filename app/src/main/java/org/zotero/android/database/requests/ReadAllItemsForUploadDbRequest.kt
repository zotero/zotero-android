package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem

class ReadAllItemsForUploadDbRequest : DbResponseRequest<RealmResults<RItem>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RItem> {
        return database.where<RItem>()
            .item(type = ItemTypes.attachment)
            .and()
            .attachmentNeedsUpload()
            .findAll()
    }
}