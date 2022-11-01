package org.zotero.android.sync

import android.webkit.MimeTypeMap
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.LinkType
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.files.FileStore
import timber.log.Timber
import java.io.File

class AttachmentCreator {

    enum class Options {
        light, dark
    }

    private sealed class FileType {
        data class contentType(val str: String): FileType()
        data class extension(val str: String): FileType()
    }

    companion object {
        fun attachmentType(item: RItem, options: Options = Options.light, fileStorage: FileStore?, urlDetector: UrlDetector?): Attachment.Kind? {
            val linkMode = item.fields.firstOrNull { it.key == FieldKeys.Item.Attachment.linkMode }?.let { LinkMode.from(it.value) }
            if (linkMode == null) {
                Timber.e("AttachmentCreator: missing link mode for item ${item.key}")
                return null
            }
            val libraryId = item.libraryId
            if (libraryId == null) {
                Timber.e("AttachmentCreator: missing library for item ${item.key}")
                return null
            }
            when (linkMode) {
                LinkMode.importedFile -> {
                    return importedType(item, libraryId = libraryId, fileStorage = fileStorage, linkType = Attachment.FileLinkType.importedFile)
                }
                LinkMode.embeddedImage -> {
                    return embeddedImageType(item, libraryId = libraryId, options = options, fileStorage = fileStorage)
                }
                LinkMode.importedUrl ->  {
                    return importedType(item, libraryId = libraryId, fileStorage = fileStorage, linkType = Attachment.FileLinkType.importedUrl)
                }

                LinkMode.linkedFile -> {
                    return linkedFileType(item = item, libraryId = libraryId)
                }

                LinkMode.linkedUrl -> {
                    if (urlDetector == null) {
                        return null
                    }
                    return linkedUrlType(item, libraryId = libraryId, urlDetector = urlDetector)
                }
            }
        }

        private fun importedType(item: RItem, libraryId: LibraryIdentifier, fileStorage: FileStore?, linkType: Attachment.FileLinkType): Attachment.Kind? {
            val contentType = contentType(item) ?: return null
            return importedType(item, contentType = contentType, libraryId = libraryId, fileStorage = fileStorage, linkType = linkType)
        }

        private fun importedType(item: RItem, contentType: String, libraryId: LibraryIdentifier, fileStorage: FileStore?, linkType: Attachment.FileLinkType): Attachment.Kind? {
            val filename = filename(item, ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType))
            val file = fileStorage!!.attachmentFile(libraryId, key = item.key, filename = filename, contentType = contentType)
            val location = location(item, file = file, fileStorage = fileStorage)
            return Attachment.Kind.file(filename = filename, contentType = contentType, location = location, linkType = linkType)
        }

        fun contentType(item: RItem): String? {
            val contentType = item.fields.firstOrNull { it.key ==  FieldKeys.Item.Attachment.contentType}?.value
            if (contentType != null && !contentType.isEmpty()) {
                return contentType
            }

            val filename = item.fields.firstOrNull {it.key == FieldKeys.Item.Attachment.filename}?.value
            if (filename != null) {
                val split = filename.split(".")
                if (split.size > 1) {
                    val ext = split.lastOrNull()?.toString()
                    if (ext != null) {
                        val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                        if (contentType != null) {
                            return contentType
                        }
                    }
                }
            }


            val title = item.fields.firstOrNull {it.key == FieldKeys.Item.Attachment.title}?.value
            if (title != null) {
                val split = title.split(".")
                if (split.size > 1) {
                    val ext = split.lastOrNull()?.toString()
                    if (ext != null) {
                        val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                        if (contentType != null) {
                            return contentType
                        }
                    }
                }
            }

            Timber.e("AttachmentCreator: contentType can't be found for ${item.key}")

            return null
        }

        private fun filename(item: RItem, ext: String?): String {
            val filename = item.fields.firstOrNull {it.key == FieldKeys.Item.Attachment.filename}?.value
            if (filename != null) {
                return filename
            }

            if (ext != null) {
                return item.displayTitle + "." + ext
            }
            return item.displayTitle
        }

        private fun location(
            item: RItem,
            file: File,
            fileStorage: FileStore?
        ): Attachment.FileLocation {
            if (fileStorage == null) {
                return Attachment.FileLocation.remote
            }
            if (file.exists()) {
                val md5 = fileStorage.md5(file)
                if (!item.backendMd5.isEmpty() && md5 != item.backendMd5) {
                    return Attachment.FileLocation.localAndChangedRemotely
                } else {
                    return Attachment.FileLocation.local
                }
                //TODO check if webdav enabled
            } else if (item.links.firstOrNull { it.type == LinkType.enclosure.name } != null) {
                return Attachment.FileLocation.remote
            } else {
                return Attachment.FileLocation.remoteMissing
            }
        }

        private fun embeddedImageType(item: RItem, libraryId: LibraryIdentifier, options: Options, fileStorage: FileStore?): Attachment.Kind? {
            val parent = item.parent
            if (parent == null) {
                Timber.e("AttachmentCreator: embedded image without parent ${item.key}")
                return null
            }

            if (parent.rawType != ItemTypes.annotation) {
                Timber.e("AttachmentCreator: embedded image with non-attachment parent ${item.key}")
                return null
            }
            val attachmentItem = parent.parent
            if (attachmentItem == null) {
                Timber.e("AttachmentCreator: embedded image ${item.key} annotation without assigned parent ${parent.key}")
                return null
            }
            val file = fileStorage!!.annotationPreview(annotationKey =  parent.key, pdfKey = attachmentItem.key, libraryId = libraryId, isDark = options == Options.dark)
            val location = location(item, file = file, fileStorage = fileStorage)
            val filename = filename(item, ext = "png")
            return Attachment.Kind.file(filename = filename, contentType = "image/png", location = location, linkType = Attachment.FileLinkType.embeddedImage)
        }

        private fun linkedUrlType(item: RItem, libraryId: LibraryIdentifier, urlDetector: UrlDetector): Attachment.Kind? {
            val urlString = item.fields.firstOrNull { it.key ==  FieldKeys.Item.Attachment.url}?.value
            if (urlString == null) {
                Timber.e("AttachmentCreator: url missing for item ${item.key}")
                return null
            }
            if (!urlDetector.isUrl(urlString)) {
                Timber.e("AttachmentCreator: url invalid '${urlString}'")
                return null
            }

            return Attachment.Kind.url(urlString)
        }

        private fun linkedFileType(item: RItem, libraryId: LibraryIdentifier): Attachment.Kind? {
            val contentType = item.fields.firstOrNull { it.key == FieldKeys.Item.Attachment.contentType }?.value
            if (contentType == null || contentType.isEmpty()) {
                Timber.e("AttachmentCreator: content type missing for item ${item.key}")
                return null
            }
            val path = item.fields.firstOrNull { it.key == FieldKeys.Item.Attachment.path }?.value
            if (path == null || path.isEmpty()) {
                Timber.e("AttachmentCreator: path missing for item ${item.key}")
                return null
            }

            val filename = filename(item, ext = File(path).extension)
            return Attachment.Kind.file(filename = filename, contentType = contentType, location = Attachment.FileLocation.local, linkType = Attachment.FileLinkType.linkedFile)
        }
    }
}