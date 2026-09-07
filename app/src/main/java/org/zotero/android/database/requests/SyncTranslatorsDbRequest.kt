package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTranslatorMetadata
import org.zotero.android.files.FileStore
import org.zotero.android.screens.share.data.TranslatorMetadata
import timber.log.Timber

class SyncTranslatorsDbRequest @AssistedInject constructor(
    @Assisted("updateMetadata") private val updateMetadata: List<TranslatorMetadata>,
    @Assisted("deleteIndices") private val deleteIndices: List<String>,
    @Assisted("forceUpdate") private val forceUpdate: Boolean,

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
            rMetadata = if (existing != null) {
                if (
                    this.forceUpdate
                    || existing.lastUpdated < metadata.lastUpdated
                    || !fileStore.translator(
                        metadata.id
                    ).exists()
                ) {
                    existing
                } else {
                    continue
                }
            } else {
                database.createObject<RTranslatorMetadata>(metadata.id)
            }
            rMetadata.lastUpdated = metadata.lastUpdated
            update.add(metadata.id to metadata.fileName)
        }
        return update
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("updateMetadata") updateMetadata: List<TranslatorMetadata>,
            @Assisted("deleteIndices") deleteIndices: List<String>,
            @Assisted("forceUpdate") forceUpdate: Boolean
        ): SyncTranslatorsDbRequest
    }

}