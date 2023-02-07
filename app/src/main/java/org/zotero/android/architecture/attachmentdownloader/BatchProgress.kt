package org.zotero.android.architecture.attachmentdownloader

import java.io.File

class BatchProgress(private val onBatchProgressUpdated: (resultProgress: Progress) -> Unit) {
    private val downloadsInProgress = mutableMapOf<File, Progress>()

    fun updateProgress(file: File, progress: Progress) {
        downloadsInProgress[file] = progress
        recalculateAndPublishProgress()
    }

    fun finishDownload(file: File) {
        downloadsInProgress.remove(file)
        recalculateAndPublishProgress()
    }

    private fun recalculateAndPublishProgress() {
        val numberOfDownloadsInProgress = downloadsInProgress.size
        val resultProgress =
            ((downloadsInProgress.values.sumOf { it.progressInHundreds } / (numberOfDownloadsInProgress * 100).toDouble()) * 100).toInt()
        onBatchProgressUpdated(Progress(resultProgress))
    }
}