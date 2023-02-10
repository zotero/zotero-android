package org.zotero.android.attachmentdownloader

interface OnDownloadProgressUpdated {

    fun onProgressUpdated(progressInHundreds: Int)
}