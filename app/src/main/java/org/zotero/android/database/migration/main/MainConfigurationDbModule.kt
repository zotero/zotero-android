package org.zotero.android.database.migration.main

import io.realm.annotations.RealmModule
import org.zotero.android.database.objects.AllItemsDbRow
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCondition
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RLink
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.RRect
import org.zotero.android.database.objects.RRelation
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.RUser
import org.zotero.android.database.objects.RVersions
import org.zotero.android.database.objects.RWebDavDeletion

@RealmModule(
    library = false, classes = [
        RCollection::class,
        RCreator::class,
        RCustomLibrary::class,
        RGroup::class,
        RItem::class,
        RItemField::class,
        RLink::class,
        RPageIndex::class,
        RPath::class,
        RPathCoordinate::class,
        RRect::class,
        RRelation::class,
        RSearch::class,
        RCondition::class,
        RTag::class,
        RTypedTag::class,
        RUser::class,
        RWebDavDeletion::class,
        RVersions::class,
        RObjectChange::class,
        AllItemsDbRow::class,
    ]
)
data class MainConfigurationDbModule(val placeholder: String) { // empty data class for equals/hashcode
    constructor(): this("")
}