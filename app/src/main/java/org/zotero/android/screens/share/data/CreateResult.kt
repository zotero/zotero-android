package org.zotero.android.screens.share.data

data class CreateResult(
    val updateParameters: Map<String, Any>,
    val changeUuids: Map<String, List<String>>,
    val md5: String,
    val mtime: Long
)