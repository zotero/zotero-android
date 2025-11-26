package org.zotero.android.attachmentdownloader

import java.util.concurrent.ConcurrentHashMap

class AttachmentBatchProgress {
    private val downloadsInProgress = ConcurrentHashMap<String, Int>()

    var currentProgress = 0

    fun updateProgress(key: String, progressInHundreds: Int) {
        downloadsInProgress[key] = progressInHundreds
        recalculateAndPublishProgress()
    }

    fun finishDownload(key: String) {
        downloadsInProgress.remove(key)
        recalculateAndPublishProgress()
    }

    private fun recalculateAndPublishProgress() {
        val numberOfDownloadsInProgress = downloadsInProgress.size
        currentProgress =
            ((downloadsInProgress.values.sumOf { it } / (numberOfDownloadsInProgress * 100).toDouble()) * 100).toInt()
    }
}