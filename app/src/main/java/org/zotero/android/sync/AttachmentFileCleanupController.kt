package org.zotero.android.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.MarkAllFilesAsNotDownloadedDbRequest
import org.zotero.android.database.requests.MarkFileAsDownloadedDbRequest
import org.zotero.android.database.requests.MarkItemsFilesAsNotDownloadedDbRequest
import org.zotero.android.database.requests.MarkLibraryFilesAsNotDownloadedDbRequest
import org.zotero.android.database.requests.ReadAllGroupsDbRequest
import org.zotero.android.database.requests.ReadAllItemsForUploadDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.ReadItemsForUploadDbRequest
import org.zotero.android.database.requests.ReadItemsWithKeysDbRequest
import org.zotero.android.database.requests.item
import org.zotero.android.files.FileStore
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentFileCleanupController @Inject constructor(
    private val fileStorage: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    dispatcher: CoroutineDispatcher,
) {
    sealed class DeletionType {
        data class individual(val attachment: Attachment, val parentKey: String?) : DeletionType()
        data class allForItems(val keys: Set<String>, val libraryId: LibraryIdentifier) :
            DeletionType()

        data class library(val libraryId: LibraryIdentifier) : DeletionType()
        object all : DeletionType()

        val notification: AttachmentFileDeletedNotification
            get() {
                return when (this) {
                    all -> AttachmentFileDeletedNotification.all
                    is library -> AttachmentFileDeletedNotification.library(this.libraryId)
                    is allForItems -> AttachmentFileDeletedNotification.allForItems(
                        keys = this.keys,
                        libraryId = libraryId
                    )
                    is individual -> AttachmentFileDeletedNotification.individual(
                        key = attachment.key,
                        parentKey = parentKey,
                        libraryId = attachment.libraryId
                    )
                }
            }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(attachmentDeleted: EventBusConstants.AttachmentDeleted) {
        delete(file = attachmentDeleted.file)
    }

    private var coroutineScope = CoroutineScope(dispatcher)

    init {
        EventBus.getDefault().register(this)
    }

    fun delete(type: DeletionType, completed: ((Boolean) -> Unit)?) {
        coroutineScope.launch {
            val newTypes = delete(type)

            for (type in newTypes) {
                EventBus.getDefault()
                    .post(EventBusConstants.AttachmentFileDeleted(type.notification))
            }
            completed?.let { it(!newTypes.isEmpty()) }
        }
    }

    private fun delete(file: File) {
        file.deleteRecursively()
    }

    private fun removeFiles(key: String, libraryId: LibraryIdentifier) {
        fileStorage.attachmentDirectory(libraryId = libraryId, key = key).deleteRecursively()
        fileStorage.annotationPreviews(pdfKey = key, libraryId = libraryId).deleteRecursively()
        fileStorage.pageThumbnails(key = key, libraryId = libraryId).deleteRecursively()
    }

    private fun delete(type: DeletionType): List<DeletionType> {
        return when (type) {
            DeletionType.all -> {
                deleteAll()
            }
            is DeletionType.allForItems -> {
                deleteAttachments(type.keys, libraryId = type.libraryId)?.let { listOf(it) }
                    ?: emptyList()
            }
            is DeletionType.library -> delete(type.libraryId)?.let { listOf(it) } ?: emptyList()
            is DeletionType.individual -> {
                return if (delete(attachment = type.attachment)) listOf(type) else emptyList()
            }
        }
    }

    private fun deleteAll(): List<DeletionType> {
        try {
            var libraryIds = listOf<LibraryIdentifier>()
            val forUpload = mutableMapOf<LibraryIdentifier, MutableList<String>>()

            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val groups = coordinator.perform(request = ReadAllGroupsDbRequest())
                libraryIds =
                    listOf(LibraryIdentifier.custom(RCustomLibraryType.myLibrary)) + groups.map {
                        LibraryIdentifier.group(it.identifier)
                    }

                for (item in coordinator.perform(request = ReadAllItemsForUploadDbRequest())) {
                    val libraryId = item.libraryId
                    if (libraryId == null) {
                        continue
                    }

                    val keys = forUpload[libraryId]
                    if (keys != null) {
                        keys.add(item.key)
                        forUpload[libraryId] = keys
                    } else {
                        forUpload[libraryId] = mutableListOf(item.key)
                    }
                }

                coordinator.perform(request = MarkAllFilesAsNotDownloadedDbRequest())
                coordinator.invalidate()
            }

            val deletedIndividually = delete(libraryIds, forUpload = forUpload)
            fileStorage.annotationPreviews.deleteRecursively()
            fileStorage.pageThumbnails.deleteRecursively()
            fileStorage.cache().deleteRecursively()

            if (deletedIndividually.isEmpty()) {
                return listOf(DeletionType.all)
            }

            return deletedIndividually.map { entry ->
                DeletionType.allForItems(entry.value, entry.key)
            }
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileCleanupController: can't remove download directory")
            return emptyList()
        }
    }


    private fun deleteAttachments(keys: Set<String>, libraryId: LibraryIdentifier): DeletionType? {
        if (keys.isEmpty()) {
            return null
        }
        try {
            val toDelete = mutableSetOf<String>()
            val toReport = mutableSetOf<String>()

            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val items = coordinator.perform(
                    request = ReadItemsWithKeysDbRequest(
                        keys = keys,
                        libraryId = libraryId
                    )
                )

                for (item in items) {
                    if (item.rawType == ItemTypes.attachment) {
                        if (item.attachmentNeedsSync) {
                            continue
                        }
                        toDelete.add(item.key)
                        toReport.add(item.key)
                        continue
                    }

                    // Or the item was a parent item and it may have multiple attachments
                    for (child in item.children!!.where().item(type = ItemTypes.attachment)
                        .findAll()) {
                        if (child.attachmentNeedsSync) {
                            continue
                        }
                        toReport.add(item.key)
                        toDelete.add(child.key)
                    }
                }

                coordinator.perform(
                    request = MarkItemsFilesAsNotDownloadedDbRequest(
                        keys = toDelete,
                        libraryId = libraryId
                    )
                )

                coordinator.invalidate()
            }

            for (key in toDelete) {
                removeFiles(key, libraryId = libraryId)
            }

            return if (toReport.isEmpty()) null else DeletionType.allForItems(toReport, libraryId)
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileCleanupController: can't remove attachments for item")
            return null
        }
    }

    private fun delete(attachment: Attachment): Boolean {
        try {
            val attachmentType = attachment.type
            if (attachmentType !is Attachment.Kind.file || attachmentType.linkType == Attachment.FileLinkType.linkedFile) {
                return false
            }

            var canDelete = false

            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val item = coordinator.perform(
                    request = ReadItemDbRequest(
                        libraryId = attachment.libraryId,
                        key = attachment.key
                    )
                )
                val attachmentNeedsSync = item.attachmentNeedsSync

                if (attachmentNeedsSync) {
                    canDelete = false
                    return@perform
                }
                coordinator.perform(
                    request = MarkFileAsDownloadedDbRequest(
                        key = attachment.key,
                        libraryId = attachment.libraryId,
                        downloaded = false
                    )
                )
                coordinator.invalidate()
                canDelete = true
            }
            if (canDelete) {
                removeFiles(attachment.key, libraryId = attachment.libraryId)
            }
            return canDelete
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileCleanupController: can't remove attachment file")
            return false
        }
    }

    private fun delete(libraryId: LibraryIdentifier): DeletionType? {
        try {
            var forUpload = listOf<String>()

            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val items =
                    coordinator.perform(request = ReadItemsForUploadDbRequest(libraryId = libraryId))
                forUpload = items.map { it.key }

                coordinator.perform(request = MarkLibraryFilesAsNotDownloadedDbRequest(libraryId = libraryId))

                coordinator.invalidate()
            }

            val deletedIndividually =
                delete(listOf(libraryId), forUpload = mapOf(libraryId to forUpload))

            fileStorage.annotationPreviews(libraryId).deleteRecursively()
            fileStorage.pageThumbnails(libraryId).deleteRecursively()
            val keys = deletedIndividually[libraryId]
            if (keys != null && !keys.isEmpty()) {
                return DeletionType.allForItems(keys, libraryId)
            }
            return DeletionType.library(libraryId)
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileCleanupController: can't remove library downloads")
            return null
        }
    }

    private fun delete(
        libraries: List<LibraryIdentifier>,
        forUpload: Map<LibraryIdentifier, List<String>>
    ): Map<LibraryIdentifier, Set<String>> {
        val deletedIndividually = mutableMapOf<LibraryIdentifier, Set<String>>()

        for (libraryId in libraries) {
            val keysForUpload = forUpload[libraryId]
            if (keysForUpload == null || keysForUpload.isEmpty()) {
                fileStorage.downloads(libraryId).deleteRecursively()
                continue
            }

            val downloadsFolder = fileStorage.downloads(libraryId)
            val contents =
                if (downloadsFolder.exists()) downloadsFolder.listFiles() else arrayOf<File>()
            if (contents.isEmpty()) {
                continue
            }

            val toDelete = contents.filter { file ->
                val key = file.name
                if (file.isDirectory && key.length == KeyGenerator.length) {
                    return@filter !keysForUpload.contains(key)
                }
                return@filter true
            }

            val keys = mutableSetOf<String>()
            for (file in toDelete) {
                val key = file.name
                if (file.isDirectory && key.length == KeyGenerator.length) {
                    keys.add(key)
                }
            }
            deletedIndividually[libraryId] = keys

            for (file in toDelete) {
                try {
                    file.deleteRecursively()
                } catch (error: Exception) {
                    Timber.e(error, "AttachmentFileCleanupController: could not remove file $file)")
                }
            }
        }
        return deletedIndividually
    }

}