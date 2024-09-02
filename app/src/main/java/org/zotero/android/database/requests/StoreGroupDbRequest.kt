package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.GroupResponse
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.GroupType
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.objects.RVersions
import timber.log.Timber

class StoreGroupDbRequest(
    val response: GroupResponse,
    val userId: Long,
):DbRequest {
    sealed class Error : Exception() {
        object unknownGroupType : Error()
    }

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val groupType = GroupType.from(response.data.type)
        if (groupType == null) {
            Timber.e("Unable to parse Group Type")
            throw Error.unknownGroupType
        }

        val group: RGroup
        val existing =
            database.where<RGroup>().equalTo("identifier", response.identifier).findFirst()
        if (existing != null) {
            group = existing
        } else {
            group = database.createObject<RGroup>(response.identifier)
            database.createEmbeddedObject(RVersions::class.java, group, "versions")
        }

        val isOwner = response.data.owner == userId
        val canEditMetadata: Boolean
        val canEditFiles: Boolean

        if (response.data.libraryEditing == "admins") {
            canEditMetadata = isOwner || (response.data.admins ?: emptyList()).contains(userId)
        } else {
            canEditMetadata = true
        }

        when (response.data.fileEditing) {
            "none" -> {
                canEditFiles = false
            }

            "admins" -> {
                canEditFiles = isOwner || (response.data.admins ?: emptyList()).contains(userId)
            }

            "members" -> {
                canEditFiles = true
            }

            else -> {
                canEditFiles = isOwner
            }
        }

        group.name = response.data.name
        group.desc = response.data.description
        group.owner = response.data.owner
        group.type = groupType.name
        group.canEditMetadata = canEditMetadata
        group.canEditFiles = canEditFiles
        group.version = response.version
        group.syncState = ObjectSyncState.synced.name
        group.isLocalOnly = false
    }
}