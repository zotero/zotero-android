package org.zotero.android.sync.syncactions

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.DeleteObjectsDbRequest
import org.zotero.android.database.requests.ReadAllAttachmentUploadsDbRequest
import org.zotero.android.database.requests.StoreItemsDbResponseRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.StoreItemsResponse

import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction
import timber.log.Timber
import java.io.FileReader

class RevertLibraryFilesSyncAction(
    private val libraryId: LibraryIdentifier,
) : SyncAction() {

    fun result() {
        Timber.i("RevertLibraryFilesSyncAction: revert files to upload")
        val toUpload =
            this.dbWrapper.realmDbStorage.perform(request = ReadAllAttachmentUploadsDbRequest(libraryId = this.libraryId))
        val cachedResponses = mutableListOf<ItemResponse>()
        val failedKeys = mutableListOf<String>()
        for (item in toUpload) {
            try {
                val file = this.fileStore.jsonCacheFile(
                    SyncObject.item,
                    libraryId = this.libraryId,
                    key = item.key
                )
                if (file.exists()) {
                    val jsonObject: JsonObject = gson.fromJson(
                        JsonReader(FileReader(file)), JsonObject::class.java
                    )
                    val jsonData = itemResponseMapper.fromJson(json = jsonObject, schemaController)
                    cachedResponses.add(jsonData)
                } else {
                    failedKeys.add(item.key)
                }
            } catch (e: Exception) {
                Timber.e(e, "RevertLibraryFilesSyncAction: can't load cached file")
                failedKeys.add(item.key)
            }
        }
        Timber.i("RevertLibraryFilesSyncAction: loaded ${cachedResponses.size} cached items, missing ${failedKeys.size}")
        Timber.i("RevertLibraryFilesSyncAction: delete files which were not uploaded yet")
        for (key in failedKeys) {
            val file = this.fileStore.attachmentDirectory(this.libraryId, key = key)
            file.delete()
        }
        var changedFilenames = mutableListOf<StoreItemsResponse.FilenameChange>()
        this.dbWrapper.realmDbStorage.perform { coordinator ->
            Timber.e("RevertLibraryFilesSyncAction: delete failed keys")
            coordinator.perform(
                request = DeleteObjectsDbRequest(
                    keys = failedKeys,
                    libraryId = this.libraryId,
                    clazz = RItem::class
                )
            )
            Timber.e("RevertLibraryFilesSyncAction: restore cached objects")
            val request = StoreItemsDbResponseRequest(
                responses = cachedResponses,
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                preferResponseData = true,
                denyIncorrectCreator = true,
            )
            changedFilenames =
                coordinator.perform(request = request).changedFilenames.toMutableList()
            coordinator.refresh()
        }

        Timber.e("RevertLibraryFilesSyncAction: rename local files to match file names")
        renameExistingFiles(changes = changedFilenames, libraryId = this.libraryId)
    }

    private fun renameExistingFiles(
        changes: List<StoreItemsResponse.FilenameChange>,
        libraryId: LibraryIdentifier
    ) {
        for (change in changes) {
            val oldFile = this.fileStore.attachmentFile(
                libraryId,
                key = change.key,
                filename = change.oldName,
            )

            if (!oldFile.exists()) {
                continue
            }

            val newFile = this.fileStore.attachmentFile(
                libraryId,
                key = change.key,
                filename = change.newName,
            )

            val renameResult = oldFile.renameTo(newFile)
            if (!renameResult) {
                Timber.w("RevertLibraryFilesSyncAction: can't rename file")
                oldFile.delete()
                newFile.delete()
            }
        }
    }

}