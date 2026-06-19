package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import org.zotero.android.database.DbRequest
import org.zotero.android.screens.share.data.TranslatorMetadata
import org.zotero.android.styles.data.Style
import timber.log.Timber

class SyncRepoResponseDbRequest @AssistedInject constructor(
    @Assisted("styles") private val styles: List<Style>,
    @Assisted("translators") private val translators: List<TranslatorMetadata>,
    @Assisted("deleteTranslators") private val deleteTranslators: List<TranslatorMetadata>,

    private val syncTranslatorsDbRequestFactory: SyncTranslatorsDbRequest.Factory,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        if (this.translators.isNotEmpty() || this.deleteTranslators.isNotEmpty()) {
            Timber.i("SyncRepoResponseDbRequest: sync translators")
            syncTranslatorsDbRequestFactory.create(
                updateMetadata = this.translators,
                deleteIndices = this.deleteTranslators.map { it.id },
                forceUpdate = false,
            ).process(database)
        }
        if (this.styles.isNotEmpty()) {
            Timber.i("SyncRepoResponseDbRequest: sync styles")
            SyncStylesDbRequest(styles = this.styles).process(database)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("styles") styles: List<Style>,
            @Assisted("translators") translators: List<TranslatorMetadata>,
            @Assisted("deleteTranslators") deleteTranslators: List<TranslatorMetadata>
        ): SyncRepoResponseDbRequest
    }
}