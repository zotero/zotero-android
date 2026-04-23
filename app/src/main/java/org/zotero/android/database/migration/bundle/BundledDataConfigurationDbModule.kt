package org.zotero.android.database.migration.bundle

import io.realm.annotations.RealmModule
import org.zotero.android.database.objects.RStyle
import org.zotero.android.database.objects.RTranslatorMetadata

@RealmModule(library = false, classes=[
    RTranslatorMetadata::class, RStyle::class
])
data class BundledDataConfigurationDbModule(val placeholder: String) { // empty data class for equals/hashcode
    constructor(): this("")
}