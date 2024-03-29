package org.zotero.android.screens.share.data

import android.webkit.MimeTypeMap
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.files.FileStore
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import java.io.File

data class UploadData(
    val type: Kind,
    val attachment: Attachment,
    val file: File,
    val filename: String,
    val libraryId: LibraryIdentifier,
    val userId: Int,
) {
        sealed interface Kind {
            data class file(val location: File, val collections: Set<String>, val tags: List<TagResponse>): Kind
            data class translated(val item: ItemResponse, val location: File): Kind
        }

        companion object {
            fun init(
                item: ItemResponse,
                attachmentKey: String,
                attachmentData: Map<String, Any>,
                attachmentFile: File,
                linkType: Attachment.FileLinkType,
                defaultTitle: String,
                libraryId: LibraryIdentifier,
                userId: Int,
                dateParser: DateParser,
                fileStore: FileStore,
            ): UploadData {
                val url = attachmentData[FieldKeys.Item.url] as? String
                val filename = FilenameFormatter.filename(
                    item,
                    defaultTitle = defaultTitle,
                    ext = attachmentFile.extension,
                    dateParser = dateParser
                )
                val file =
                    fileStore.attachmentFile(libraryId, key = attachmentKey, filename = filename)
                val contentType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(attachmentFile.extension)!!
                val attachment = Attachment(
                    type = Attachment.Kind.file(
                        filename = filename,
                        contentType = contentType,
                        location = Attachment.FileLocation.local,
                        linkType = linkType
                    ),
                    title = filename,
                    url = url,
                    key = attachmentKey,
                    libraryId = libraryId
                )
                return UploadData(
                    type = Kind.translated(item = item, location = attachmentFile),
                    attachment = attachment,
                    file = file,
                    filename = filename,
                    libraryId = libraryId,
                    userId = userId
                )
            }

            fun init(
                file: File,
                filename: String,
                attachmentKey: String,
                linkType: Attachment.FileLinkType,
                remoteUrl: String?,
                collections: Set<String>,
                tags: List<TagResponse>,
                libraryId: LibraryIdentifier,
                userId: Int,
                fileStore: FileStore,
            ): UploadData {
                val newFile =
                    fileStore.attachmentFile(libraryId, key = attachmentKey, filename = filename)
                val contentType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)!!
                val attachment = Attachment(
                    type = Attachment.Kind.file(
                        filename = filename,
                        contentType = contentType,
                        location = Attachment.FileLocation.local,
                        linkType = linkType
                    ),
                    title = filename,
                    url = remoteUrl,
                    key = attachmentKey,
                    libraryId = libraryId
                )

                return UploadData(
                    type = Kind.file(location = file, collections = collections, tags = tags),
                    attachment = attachment,
                    file = newFile,
                    filename = filename,
                    libraryId = libraryId,
                    userId = userId
                )
            }
        }
}