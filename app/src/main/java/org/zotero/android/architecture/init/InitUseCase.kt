package org.zotero.android.architecture.init

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.files.FileStore
import javax.inject.Inject

class InitUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val fileStore: FileStore,
    private val dbWrapper: DbWrapper,
) {
    suspend fun execute(
        context: Context
    ) = withContext(dispatcher) {
        fileStore.init()
        dbWrapper.initDb(context)

    }
}
