package org.zotero.android.helpers

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Gets a mime type for the given file in the Android content resolver.
 */
class GetMimeTypeUseCase @Inject constructor(
    private val application: Application,
    private val dispatcher: CoroutineDispatcher
) {
    suspend fun execute(uri: String): MimeType? = withContext(dispatcher) {
        val contentUri = Uri.parse(uri)
        application.contentResolver.getType(contentUri)
    }
}
