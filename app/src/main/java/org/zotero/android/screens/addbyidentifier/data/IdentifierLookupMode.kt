package org.zotero.android.screens.addbyidentifier.data

sealed interface IdentifierLookupMode {
    object normal : IdentifierLookupMode
    object identifyOnly : IdentifierLookupMode
    object identifyAndSaveParentItem : IdentifierLookupMode
}