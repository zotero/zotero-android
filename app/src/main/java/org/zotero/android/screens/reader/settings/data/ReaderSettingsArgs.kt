package org.zotero.android.screens.reader.settings.data

import org.zotero.android.screens.reader.data.ReaderFileType

data class ReaderSettingsArgs(
    val readerSettings: ReaderSettings,
    val fileType: ReaderFileType,
)