package org.zotero.android.architecture

import android.net.Uri

object EventBusConstants {
    data class FileWasSelected(
        val uri: Uri?
    )
}