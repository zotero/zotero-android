package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.files.FileStore
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.AttachmentUpload
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.io.File

class ReadAttachmentUploadsDbRequest(
    val libraryId: LibraryIdentifier,
    val fileStorage: FileStore
) : DbResponseRequest<List<AttachmentUpload>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
    ): List<AttachmentUpload> {
        val items = database
            .where<RItem>()
            .itemsNotChangedAndNeedUpload(this.libraryId)
            .findAll()
        val uploads = items.mapNotNull { item ->
            val contentType =
                item.fields
                    .where()
                    .key(FieldKeys.Item.Attachment.contentType)
                    .findFirst()?.value
            if (contentType == null) {
                Timber.e("ReadAttachmentUploadsDbRequest: contentType field missing !!!")
                return@mapNotNull null
            }
            val mtimeField = item.fields
                .where()
                .key(FieldKeys.Item.Attachment.mtime)
                .findFirst()
            if (mtimeField == null) {
                Timber.e("ReadAttachmentUploadsDbRequest: mtime field missing !!!")
                return@mapNotNull null
            }
            val mtime = mtimeField.value.toLongOrNull()
            if (mtime == null) {
                Timber.e("ReadAttachmentUploadsDbRequest: mtime field " +
                        "value not a number ${mtimeField.value} !!!")
                return@mapNotNull null
            }
            val md5Field = item.fields
                .where()
                .key(FieldKeys.Item.Attachment.md5)
                .findFirst()
            if (md5Field == null) {
                Timber.e("ReadAttachmentUploadsDbRequest: md5 field missing !!!")
                return@mapNotNull null
            }

            val attachmentType = AttachmentCreator.attachmentType(
                item,
                options = AttachmentCreator.Options.light,
                fileStorage = fileStorage,
                isForceRemote = true,
                urlDetector = null
            )
            if (attachmentType == null) {
                return@mapNotNull null
            }

            val filename: String
            val file: File
            when (attachmentType) {
                is Attachment.Kind.url ->
                    return@mapNotNull null
                is Attachment.Kind.file -> {
                    // Don't try to upload linked attachments
                    if (attachmentType.linkType == Attachment.FileLinkType.linkedFile) {
                        return@mapNotNull null
                    }
                    file = fileStorage.attachmentFile(
                        this.libraryId,
                        key = item.key,
                        filename = attachmentType.filename,
                        contentType = attachmentType.contentType
                    )
                    filename = attachmentType.filename
                }
            }
            if (md5Field.value == "<null>") {
                val newMd5 = fileStorage.md5(file)
                md5Field.value = newMd5
            }
            var backendMd5: String? = if (item.backendMd5.isEmpty()) null else item.backendMd5
            if (backendMd5 == "<null>") {
                // Don't need to update item here, it'll get updated in `MarkAttachmentUploadedDbRequest`.
                backendMd5 = null
            }
            AttachmentUpload(
                libraryId = this.libraryId,
                key = item.key,
                filename = filename,
                contentType = contentType,
                md5 = md5Field.value,
                mtime = mtime,
                file = file,
                oldMd5 = backendMd5
            )
        }
        return uploads
    }
}