package org.zotero.android.database.requests

import io.realm.Realm
import org.zotero.android.database.DbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.share.data.TranslatorMetadata
import org.zotero.android.styles.data.Style
import timber.log.Timber

class SyncRepoResponseDbRequest(
    private val styles: List<Style>,
    private val translators: List<TranslatorMetadata>,
    private val deleteTranslators: List<TranslatorMetadata>,
    private val fileStore: FileStore
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        if (this.translators.isNotEmpty() || this.deleteTranslators.isNotEmpty()) {
            Timber.i("SyncRepoResponseDbRequest: sync translators")
            SyncTranslatorsDbRequest(
                updateMetadata = this.translators,
                deleteIndices = this.deleteTranslators.map { it.id },
                forceUpdate = false,
                fileStore = this.fileStore
            ).process(database)
        }
        if (this.styles.isNotEmpty()) {
            Timber.i("SyncRepoResponseDbRequest: sync styles")
            SyncStylesDbRequest(styles = this.styles).process(database)
        }
    }
}