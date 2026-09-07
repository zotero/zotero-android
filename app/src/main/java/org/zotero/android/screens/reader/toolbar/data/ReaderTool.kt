package org.zotero.android.screens.reader.toolbar.data

import org.zotero.android.screens.reader.data.ReaderAnnotationTool

data class ReaderTool(
    val type: ReaderAnnotationTool,
    val title: Int,
    val image: Int,
    val isHidden: Boolean,
)