package org.zotero.android.attachmentdownloader

import android.content.Context
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

// Starts AttachmentDownloadService when a batch download starts. The service stops itself once
// the batch finishes
@Singleton
class AttachmentDownloadNotificationController @Inject constructor(
    private val context: Context,
    private val attachmentDownloader: AttachmentDownloader,
    private val applicationScope: ApplicationScope,
) {
    fun init() {
        attachmentDownloader.batchState
            .map { it != null }
            .distinctUntilChanged()
            .filter { it }
            .onEach { AttachmentDownloadService.start(context) }
            .launchIn(applicationScope)
    }
}
