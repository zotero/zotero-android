package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier

class ReadAnnotationsDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
) : DbResponseRequest<RealmResults<RItem>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RItem> {
        val supportedTypes =
            AnnotationType.entries
                .filter { AnnotationsConfig.supported.contains(it.kind) }
                .map { it.name }
        return database.where<RItem>()
            .parent(this.attachmentKey, this.libraryId)
            .items(type = ItemTypes.annotation, notSyncState = ObjectSyncState.dirty)
            .deleted(false)
            .rawPredicate("annotationType in %@", supportedTypes)
            .sort("annotationSortIndex", Sort.ASCENDING)
            .findAll()
    }
}