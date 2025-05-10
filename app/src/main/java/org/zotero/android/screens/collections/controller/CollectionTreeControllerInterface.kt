package org.zotero.android.screens.collections.controller

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.sync.CollectionIdentifier

interface CollectionTreeControllerInterface {
    fun sendChangesToUi(
        listOfCollectionItemsWithChildren: PersistentList<CollectionItemWithChildren>?,
        collapsed: PersistentMap<CollectionIdentifier, Boolean>?
    )
}