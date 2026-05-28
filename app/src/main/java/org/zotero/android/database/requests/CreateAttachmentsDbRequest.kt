package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.files.FileStore
import timber.log.Timber

class CreateAttachmentsDbRequest @AssistedInject constructor(
    @Assisted("attachments") private val attachments: List<Attachment>,
    @Assisted("parentKey") private val parentKey: String?,
    @Assisted("localizedType") private val localizedType: String,
    @Assisted("collections") private val collections: Set<String>,

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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("attachments") attachments: List<Attachment>,
            @Assisted("parentKey") parentKey: String?,
            @Assisted("localizedType") localizedType: String,
            @Assisted("collections") collections: Set<String>
        ): CreateAttachmentsDbRequest
    }
}