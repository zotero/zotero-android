package org.zotero.android.screens.filter.data

import org.zotero.android.screens.allitems.data.ItemsState

sealed class FilterResult {
    data class enableFilter(val filter: ItemsState.Filter): FilterResult()
    data class disableFilter(val filter: ItemsState.Filter): FilterResult()
}