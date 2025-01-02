package org.zotero.android.sync

import android.webkit.MimeTypeMap
import org.zotero.android.androidx.file.copyWithExt
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.LinkType
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.files.FileStore
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.Int

class AttachmentCreator {

    enum class Options {
        light, dark
    }

    private sealed class FileType {
        data class contentType(val str: String): FileType()
        data class extension(val str: String): FileType()
    }

    companion object {
        private val mainAttachmentContentTypes = setOf("text/html", "application/pdf", "image/png", "image/jpeg", "image/gif", "text/plain")

        fun mainAttachment(item: RItem, fileStorage: FileStore, defaults: Defaults): Attachment? {
            if (item.rawType == ItemTypes.attachment) {
                val attachment = attachment(
                    item = item,
                    fileStorage = fileStorage,
                    urlDetector = null,
                    isForceRemote = false,
                    defaults = defaults,
                )
                if (attachment != null) {
                    when {
                        attachment.type is Attachment.Kind.url ->
                            return attachment
                        attachment.type is Attachment.Kind.file && (attachment.type.linkType == Attachment.FileLinkType.importedFile || attachment.type.linkType == Attachment.FileLinkType.importedUrl) ->
                            return attachment
                        else -> {
                            //no-op
                        }
                    }
                }
                return null
            }

            var attachmentData = attachmentData(item)

            if (attachmentData.isEmpty()) {
                return null
            }

            attachmentData = sortAttachmentData(attachmentData)

            val firstAttachmentData = attachmentData.firstOrNull()
            if (firstAttachmentData == null) {
                return null
            }
            val idx = firstAttachmentData.idx
            val contentType = firstAttachmentData.contentType
            val linkMode = firstAttachmentData.linkMode

            val rAttachment = item.children?.get(idx)
            val linkType: Attachment.FileLinkType =
                if (linkMode == LinkMode.importedFile) Attachment.FileLinkType.importedFile else Attachment.FileLinkType.importedUrl


            val libraryId = rAttachment?.libraryId
            if (libraryId != null) {
                val type = importedType(
                    item = rAttachment,
                    contentType = contentType,
                    libraryId = libraryId,
                    fileStorage = fileStorage,
                    isForceRemote = false,
                    linkType = linkType,
                    defaults = defaults,
                )
                if (type != null) {
                    return Attachment.initWithItemAndKind(item = rAttachment, type = type)
                }
            }
            return null
        }

        private fun attachmentData(item: RItem): List<AttachmentData> {
            val itemUrl = item.fields.firstOrNull{it.key == FieldKeys.Item.url }?.value
            var data = mutableListOf<AttachmentData>()
            item.children!!
            for ((idx, child) in item.children.freeze().withIndex()) {
                if (child.rawType == ItemTypes.attachment && child.syncState != ObjectSyncState.dirty.name && !child.trash) {
                    val linkMode = child.fields.firstOrNull { it.key == FieldKeys.Item.Attachment.linkMode }?.let{ LinkMode.from(it.value) }
                    if (linkMode == LinkMode.importedUrl || linkMode == LinkMode.importedFile) {
                        val contentType = contentType(child)
                        if (contentType != null && mainAttachmentContentTypes.contains(contentType)) {
                            var hasMatchingUrlWithParent = false
                            val url = itemUrl
                            if (url != null) {
                                val childUrl = child.fields.firstOrNull{it.key == FieldKeys.Item.Attachment.url }?.value
                                if (childUrl != null) {
                                    hasMatchingUrlWithParent = url == childUrl
                                }
                            }
                            data.add(AttachmentData(idx, contentType, linkMode, hasMatchingUrlWithParent, child.dateAdded))
                        }
                    }
                }
            }

            return data
        }


        fun attachment(
            item: RItem,
            options: Options = Options.light,
            fileStorage: FileStore,
            isForceRemote: Boolean,
            urlDetector: UrlDetector?,
            defaults: Defaults,
        ): Attachment? {
            return attachmentType(
                item,
                options = options,
                fileStorage = fileStorage,
                urlDetector = urlDetector,
                isForceRemote = isForceRemote,
                defaults = defaults,
            )?.let { Attachment.initWithItemAndKind(item = item, type = it) }
        }

        fun attachmentType(
            item: RItem,
            options: Options = Options.light,
            fileStorage: FileStore,
            isForceRemote: Boolean,
            urlDetector: UrlDetector?,
            defaults: Defaults,
        ): Attachment.Kind? {
            val linkMode = item.fields.firstOrNull { it.key == FieldKeys.Item.Attachment.linkMode }
                ?.let { LinkMode.from(it.value) }
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
                    return importedType(
                        item = item,
                        libraryId = libraryId,
                        fileStorage = fileStorage,
                        isForceRemote = isForceRemote,
                        linkType = Attachment.FileLinkType.importedFile,
                        defaults = defaults,
                    )
                }
                LinkMode.embeddedImage -> {
                    return embeddedImageType(
                        item,
                        libraryId = libraryId,
                        options = options,
                        isForceRemote = isForceRemote,
                        fileStorage = fileStorage,
                        defaults = defaults,
                    )
                }
                LinkMode.importedUrl -> {
                    return importedType(
                        item = item,
                        libraryId = libraryId,
                        fileStorage = fileStorage,
                        isForceRemote = isForceRemote,
                        linkType = Attachment.FileLinkType.importedUrl,
                        defaults = defaults,
                    )
                }

                LinkMode.linkedFile -> {
                    return linkedFileType(item = item)
                }

                LinkMode.linkedUrl -> {
                    if (urlDetector == null) {
                        return null
                    }
                    return linkedUrlType(item = item, urlDetector = urlDetector)
                }
            }
        }

        private fun importedType(
            item: RItem,
            libraryId: LibraryIdentifier,
            fileStorage: FileStore,
            isForceRemote: Boolean,
            linkType: Attachment.FileLinkType,
            defaults: Defaults,
        ): Attachment.Kind? {
            val contentType = contentType(item) ?: return null
            return importedType(
                item = item,
                contentType = contentType,
                libraryId = libraryId,
                fileStorage = fileStorage,
                isForceRemote = isForceRemote,
                linkType = linkType,
                defaults = defaults,
            )
        }

        private fun importedType(
            item: RItem,
            contentType: String,
            libraryId: LibraryIdentifier,
            fileStorage: FileStore,
            isForceRemote: Boolean,
            linkType: Attachment.FileLinkType,
            defaults: Defaults,
        ): Attachment.Kind {
            val filename = filename(
                item,
                ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
            )
            val file = fileStorage.attachmentFile(
                libraryId = libraryId,
                key = item.key,
                filename = filename,
            )
            val location = location(
                item = item,
                file = file,
                fileStorage = fileStorage,
                isForceRemote = isForceRemote,
                defaults = defaults
            )
            return Attachment.Kind.file(
                filename = filename,
                contentType = contentType,
                location = location,
                linkType = linkType
            )
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
            fileStorage: FileStore,
            isForceRemote: Boolean,
            defaults: Defaults,
        ): Attachment.FileLocation {
            if (isForceRemote) {
                return Attachment.FileLocation.remote
            }
            val webDavEnabled = defaults.isWebDavEnabled()
            if (file.exists()|| (webDavEnabled && file.copyWithExt("zip").exists())) {
                val md5 = fileStorage.md5(file)
                if (!item.backendMd5.isEmpty() && md5 != item.backendMd5) {
                    return Attachment.FileLocation.localAndChangedRemotely
                } else {
                    return Attachment.FileLocation.local
                }
            } else if (webDavEnabled || item.links.firstOrNull { it.type == LinkType.enclosure.name } != null) {
                return Attachment.FileLocation.remote
            } else {
                return Attachment.FileLocation.remoteMissing
            }
        }

        private fun embeddedImageType(
            item: RItem,
            libraryId: LibraryIdentifier,
            options: Options,
            fileStorage: FileStore,
            isForceRemote: Boolean,
            defaults: Defaults,
        ): Attachment.Kind? {
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
            val file = fileStorage.annotationPreview(
                annotationKey = parent.key,
                pdfKey = attachmentItem.key,
                libraryId = libraryId,
                isDark = options == Options.dark
            )
            val location = location(
                item = item,
                file = file,
                fileStorage = fileStorage,
                isForceRemote = isForceRemote,
                defaults = defaults
            )
            val filename = filename(item, ext = "png")
            return Attachment.Kind.file(
                filename = filename,
                contentType = "image/png",
                location = location,
                linkType = Attachment.FileLinkType.embeddedImage
            )
        }

        private fun linkedUrlType(item: RItem, urlDetector: UrlDetector): Attachment.Kind? {
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

        private fun sortAttachmentData(unsortedAttachmentData: List<AttachmentData>): List<AttachmentData> {
            val debugData = unsortedAttachmentData.map {
                "Triple(\"${it.contentType}\", ${it.hasMatchingUrlWithParent}, Date(${it.dateAdded.time}))"
            }.joinToString(separator = ", ") { it }

            try {
                return mainAttachmentsAreInIncreasingOrderV1(unsortedAttachmentData)
            } catch (e: Exception) {
                Timber.e(e, "Sorting attachmentData using V1 method failed. Debug data = $debugData")
            }

            Timber.w("Attempting to sort attachmentData with V2 method")

            try {
                return mainAttachmentsAreInIncreasingOrderV2(unsortedAttachmentData)
            } catch (e: Exception) {
                Timber.e(e, "Sorting attachmentData using V2 method failed. Debug data = $debugData")
            }
            Timber.w("Returning unsorted attachmentData as a last resort")
            return unsortedAttachmentData
        }

        private fun mainAttachmentsAreInIncreasingOrderV1(unsortedAttachmentData: List<AttachmentData>): List<AttachmentData> {
            return unsortedAttachmentData.sortedWith { lData, rData ->
                val lPriority = priority(lData.contentType)
                val rPriority = priority(rData.contentType)

                if (lPriority != rPriority) {
                    return@sortedWith lPriority.compareTo(rPriority)
                }

                if (lData.hasMatchingUrlWithParent != rData.hasMatchingUrlWithParent) {
                    return@sortedWith (lData.hasMatchingUrlWithParent).compareTo(!(rData.hasMatchingUrlWithParent))
                }
                return@sortedWith lData.dateAdded.compareTo(rData.dateAdded)
            }
        }

        private fun mainAttachmentsAreInIncreasingOrderV2(unsortedAttachmentData: List<AttachmentData>): List<AttachmentData> {
            return unsortedAttachmentData.sortedWith { lData, rData ->
                val lPriority = priority(lData.contentType)
                val rPriority = priority(rData.contentType)

                if (lPriority != rPriority) {
                    return@sortedWith lPriority.compareTo(rPriority)
                }

                if (lData.hasMatchingUrlWithParent != rData.hasMatchingUrlWithParent) {
                    return@sortedWith (lData.hasMatchingUrlWithParent).compareTo((rData.hasMatchingUrlWithParent))
                }
                return@sortedWith lData.dateAdded.compareTo(rData.dateAdded)
            }
        }


        private fun linkedFileType(item: RItem): Attachment.Kind? {
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
        private fun priority(contentType: String): Int {
            return when (contentType) {
                "application/pdf" -> 0
                "text/html" -> 1
                "image/gif", "image/jpeg", "image/png" -> 2
                "text/plain" -> 3
                else -> 4
            }
        }
    }
}

private data class AttachmentData(val idx: Int, val contentType: String, val linkMode: LinkMode, val hasMatchingUrlWithParent: Boolean ,val dateAdded: Date)