package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RGroup

class MigrateMarkAllNonLocalGroupsAsOutdatedToTriggerResync(private val dynamicRealm: DynamicRealm) {

    fun migrate() {
        val groups = dynamicRealm
            .where(RGroup::class.java.simpleName)
            .equalTo("isLocalOnly", false)
            .findAll()
        for (group in groups) {
            group.setString("syncState", ObjectSyncState.outdated.name)
        }
    }

}