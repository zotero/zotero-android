package org.zotero.android.api.pojo.sync

import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.sync.LibraryIdentifier

data class LastReadAloudPositionResponse(
    val key: String,
    val value: ReadAloudResumePosition,
    val version: Int,
    val libraryId: LibraryIdentifier,
)

data class LastReadAloudPositionsResponse(
    val positions: List<LastReadAloudPositionResponse>,
)