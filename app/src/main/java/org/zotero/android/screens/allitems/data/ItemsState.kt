package org.zotero.android.screens.allitems.data

import org.zotero.android.sync.Tag

class ItemsState {
    data class DownloadBatchData(
        val fraction: Int,
        val downloaded: Int,
        val total: Int,
    ) {
        companion object {
            fun init(progress: Int, remaining: Int, total: Int): DownloadBatchData? {
                if (total <= 1) {
                    return null
                }
                return DownloadBatchData(
                    fraction = progress,
                    downloaded = total - remaining,
                    total = total
                )
            }
        }
    }

    sealed class Filter {
        object downloadedFiles : Filter()
        data class tags(val tags: List<Tag>) : Filter()
    }
}