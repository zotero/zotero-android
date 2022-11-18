package org.zotero.android.dashboard.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.zotero.android.architecture.database.objects.Attachment

sealed class DetailType: Parcelable {
    @Parcelize
    data class creation(val type: String, val child: Attachment?, val collectionKey: String?): DetailType()

    @Parcelize
    data class duplication(val itemKey: String, val collectionKey: String?): DetailType()

    @Parcelize
    data class preview(val key: String): DetailType()

    val previewKey: String? get() {
        when(this) {
            is DetailType.preview -> return this.key
            is DetailType.duplication, is DetailType.creation -> return null
        }
    }
}