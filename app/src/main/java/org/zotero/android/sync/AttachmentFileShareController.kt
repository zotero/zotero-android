package org.zotero.android.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider.getUriForFile
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.requests.ReadItemsWithKeysDbRequest
import org.zotero.android.database.requests.item
import org.zotero.android.files.FileStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentFileShareController @Inject constructor(
    private val fileStore: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context
) {
    sealed class ShareType {
        data class individual(val attachment: Attachment, val parentKey: String?) : ShareType()
        data class allForItems(val keys: Set<String>, val libraryId: LibraryIdentifier) :
            ShareType()

        data class library(val libraryId: LibraryIdentifier) : ShareType()
        object all : ShareType()
    }

    fun share(type: ShareType): List<ShareType> {
        return when (type) {
            is ShareType.allForItems -> {
                shareDownloadedAttachments(
                    type.keys,
                    libraryId = type.libraryId
                )?.let { listOf(it) }
                    ?: emptyList()
            }
            is ShareType.individual -> {
                return if (share(attachment = type.attachment)) listOf(type) else emptyList()
            }
            else -> {
                // TODO: Implement the other two sharing types
                return emptyList()
            }
        }
    }

    fun share(attachment: Attachment): Boolean {
        try {
            when(val attachmentType = attachment.type) {
                is Attachment.Kind.url -> {
                    // This is currently unused, but is implemented anyway
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, attachmentType.url)
                        type = "text/plain"
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, null)
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
                is Attachment.Kind.file -> {
                    val file = fileStore.attachmentFile(
                        libraryId = attachment.libraryId,
                        key = attachment.key,
                        filename = attachmentType.filename,
                    )
                    val uri = getUriForFile(context, context.packageName + ".provider", file)
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = attachmentType.contentType
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, null)
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
            }
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileShareController: can't share attachments for item")
            return false
        }
        return true
    }

    private fun shareDownloadedAttachments(
        keys: Set<String>,
        libraryId: LibraryIdentifier
    ): ShareType? {
        if (keys.isEmpty()) {
            return null
        }
        try {
            val toShare = mutableSetOf<String>()

            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val items = coordinator.perform(
                    request = ReadItemsWithKeysDbRequest(
                        keys = keys,
                        libraryId = libraryId
                    )
                )

                for (item in items) {
                    if (item.rawType == ItemTypes.attachment) {
                        if (!item.fileDownloaded) continue // Attachment is not downloaded, skip
                        toShare.add(item.key)
                        continue
                    }

                    // Or the item was a parent item and it may have multiple attachments
                    for (child in item.children!!.where().item(type = ItemTypes.attachment)
                        .findAll()) {
                        if (!child.fileDownloaded) continue // Attachment is not downloaded, skip
                        toShare.add(child.key)
                    }
                }

                coordinator.invalidate()
            }

            shareFiles(toShare, libraryId = libraryId)

            return if (toShare.isEmpty()) {
                null
            } else {
                ShareType.allForItems(
                    keys = toShare,
                    libraryId = libraryId
                )
            }
        } catch (error: Exception) {
            Timber.e(error, "AttachmentFileShareController: can't share attachments for item")
            return null
        }
    }

    private fun shareFiles(keys: Iterable<String>, libraryId: LibraryIdentifier) {
        val uris = mutableListOf<Uri>()
        for (key in keys) {
            val dir = fileStore.attachmentDirectory(libraryId, key)
            val files = dir.listFiles()
            if (files == null) {
                Timber.e("AttachmentFileShareController: argument to shareFiles is not a directory: %s", dir)
                continue
            }
            for (file in files) {
                val uri = getUriForFile(context, context.packageName + ".provider", file)
                uris.add(uri)
            }
        }
        if (uris.size == 0) return
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "application/octet-stream"
        }
        val chooserIntent = Intent.createChooser(shareIntent, null)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}