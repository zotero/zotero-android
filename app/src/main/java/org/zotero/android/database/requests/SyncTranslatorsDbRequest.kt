package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTranslatorMetadata
import org.zotero.android.files.FileStore
import org.zotero.android.screens.share.data.TranslatorMetadata
import timber.log.Timber

class SyncTranslatorsDbRequest(
    private val updateMetadata: List<TranslatorMetadata>,
    private val deleteIndices: List<String>,
    private val forceUpdate: Boolean,
    private val fileStore: FileStore,
) : DbResponseRequest<List<Pair<String, String>>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): List<Pair<String, String>> {
        if (this.deleteIndices.isNotEmpty()) {
            Timber.i("SyncTranslatorsDbRequest: delete ${this.deleteIndices.size} translators")
            val objects =
                database
                    .where<RTranslatorMetadata>()
                    .`in`("id", this.deleteIndices.toTypedArray())
                    .findAllAsync()
            objects.deleteAllFromRealm()
        }
        val update = mutableListOf<Pair<String, String>>()
        Timber.i("SyncTranslatorsDbRequest: update ${this.updateMetadata.size} translators")
        for (metadata in this.updateMetadata) {
            val rMetadata: RTranslatorMetadata
            val existing =
                database.where<RTranslatorMetadata>().equalTo("id", metadata.id).findFirst()
            if (existing != null) {
                if (
                    this.forceUpdate
                    || existing.lastUpdated < metadata.lastUpdatedDate
                    || !fileStore.translator(
                        metadata.id
                    ).exists()
                ) {
                    rMetadata = existing
                } else {
                    continue
                }
            } else {
                rMetadata = database.createObject<RTranslatorMetadata>(metadata.id)
            }
            rMetadata.lastUpdated = metadata.lastUpdatedDate
            update.add(metadata.id to metadata.fileName)
        }
        return update
    }
}