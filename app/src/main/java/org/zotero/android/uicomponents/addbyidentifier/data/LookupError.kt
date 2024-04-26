package org.zotero.android.uicomponents.addbyidentifier.data

sealed class LookupError: Exception() {
    object cantFindFile: LookupError()
    object invalidIdentifiers: LookupError()
    object noSuccessfulTranslators: LookupError()
    object lookupFailed: LookupError()
}