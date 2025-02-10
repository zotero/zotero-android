package org.zotero.android.pdfworker.data

import org.zotero.android.sync.LibraryIdentifier

sealed interface PdfWorkerMode {
    data class recognizeAndSave(
        var itemKey: String,
        var libraryIdentifier: LibraryIdentifier,
        var collections: Set<String>
    ) : PdfWorkerMode

    object recognizeAndWait : PdfWorkerMode
}