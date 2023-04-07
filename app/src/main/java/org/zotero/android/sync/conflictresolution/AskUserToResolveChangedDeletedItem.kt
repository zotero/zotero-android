package org.zotero.android.sync.conflictresolution

import org.zotero.android.screens.dashboard.ConflictDialogData

data class AskUserToResolveChangedDeletedItem(val conflictDataList: List<ConflictDialogData.changedItemsDeletedAlert>)