package org.zotero.android.screens.filter.data

import org.zotero.android.screens.allitems.data.ItemsFilter

sealed class FilterResult {
    data class enableFilter(val filter: ItemsFilter): FilterResult()
    data class disableFilter(val filter: ItemsFilter): FilterResult()
}