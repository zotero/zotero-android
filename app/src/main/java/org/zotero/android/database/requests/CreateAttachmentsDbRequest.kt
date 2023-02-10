package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.files.FileStore
import timber.log.Timber

class CreateAttachmentsDbRequest(
    val attachments: List<Attachment>,
    val parentKey: String?,
    val localizedType: String,
    val collections: Set<String>,
    val fileStore: FileStore
) : DbResponseRequest<List<Pair<String, String>>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
    ): List<Pair<String, String>> {

        val libraryId = this.attachments.firstOrNull()?.libraryId ?: return emptyList()

        val parent = this.parentKey?.let {
            database
                .where<RItem>()
                .key(it, libraryId)
                .findFirst()
        }
        if (parent != null) {
            parent.version = parent.version
        }

        val failed: MutableList<Pair<String, String>> = mutableListOf()

        for (attachment in this.attachments) {
            try {
                val attachment = CreateAttachmentDbRequest(
                    attachment = attachment,
                    parentKey = null,
                    localizedType = this.localizedType,
                    includeAccessDate = attachment.hasUrl,
                    collections = this.collections,
                    tags = emptyList(),
                    fileStore = fileStore
                ).process(database)
                if (parent != null) {
                    attachment.parent = parent
                    attachment.changes.add(
                        RObjectChange.create(changes = listOf(RItemChanges.parent))
                    )
                }
            } catch (error: Throwable) {
                Timber.e(error, "CreateAttachmentsDbRequest: could not create attachment ")
                failed.add((attachment.key to attachment.title))
            }
        }

        return failed
    }
}