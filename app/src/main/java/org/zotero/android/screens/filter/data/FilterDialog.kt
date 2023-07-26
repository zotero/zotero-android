package org.zotero.android.screens.filter.data

sealed interface FilterDialog {
    data class confirmDeletion(val count: Int) : FilterDialog
}