package org.zotero.android.sync.syncactions

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.DeleteObjectsDbRequest
import org.zotero.android.database.requests.ReadAllAttachmentUploadsDbRequest
import org.zotero.android.database.requests.StoreItemsDbResponseRequest
import org.zotero.android.files.FileStore
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.StoreItemsResponse
import org.zotero.android.sync.SyncAction
import org.zotero.android.sync.SyncObject
import timber.log.Timber
import java.io.FileReader

class RevertLibraryFilesSyncAction(
    private val libraryId: LibraryIdentifier,
    private val dbStorage: DbWrapper,
    private val fileStorage: FileStore,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
    private val gson: Gson,
    private val itemResponseMapper: ItemResponseMapper,
) : SyncAction<Unit> {

    override suspend fun result() {
        Timber.i("RevertLibraryFilesSyncAction: revert files to upload")
        val toUpload =
            dbStorage.realmDbStorage.perform(request = ReadAllAttachmentUploadsDbRequest(libraryId = this.libraryId))
        val cachedResponses = mutableListOf<ItemResponse>()
        val failedKeys = mutableListOf<String>()
        for (item in toUpload) {
            try {
                val file = fileStorage.jsonCacheFile(
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
            val file = fileStorage.attachmentDirectory(this.libraryId, key = key)
            file.delete()
        }
        var changedFilenames = mutableListOf<StoreItemsResponse.FilenameChange>()
        dbStorage.realmDbStorage.perform { coordinator ->
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
                preferResponseData = true
            )
            changedFilenames =
                coordinator.perform(request = request).changedFilenames.toMutableList()
            coordinator.invalidate()
        }

        Timber.e("RevertLibraryFilesSyncAction: rename local files to match file names")
        renameExistingFiles(changes = changedFilenames, libraryId = this.libraryId)
    }

    private fun renameExistingFiles(
        changes: List<StoreItemsResponse.FilenameChange>,
        libraryId: LibraryIdentifier
    ) {
        for (change in changes) {
            val oldFile = fileStorage.attachmentFile(
                libraryId,
                key = change.key,
                filename = change.oldName,
                contentType = change.contentType
            )

            if (!oldFile.exists()) {
                continue
            }

            val newFile = fileStorage.attachmentFile(
                libraryId,
                key = change.key,
                filename = change.newName,
                contentType = change.contentType
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