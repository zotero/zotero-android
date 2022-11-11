package org.zotero.android.helpers

import org.zotero.android.architecture.Result
import javax.inject.Inject

class SelectMediaUseCase @Inject constructor(
    private val getMimeType: GetMimeTypeUseCase,
    private val saveFileToInternalStorage: SaveFileToInternalStorageUseCase
) {
    suspend fun execute(
        uri: String?,
        isValidMimeType: (MimeType) -> Boolean
    ): MediaSelectionResult {
        return if (uri == null) {
            MediaSelectionResult.NullMedia
        } else {
            val mimeType: MimeType? = getMimeType.execute(uri)
            val validMimeType = mimeType != null && isValidMimeType(mimeType)
            if (validMimeType) {
                when (val saveResult = saveFileToInternalStorage.execute(uri)) {
                    is Result.Failure -> MediaSelectionResult.AttachMediaError(saveResult.exception)
                    is Result.Success -> MediaSelectionResult.AttachMediaSuccess(saveResult.value)
                }
            } else {
                MediaSelectionResult.InvalidMedia
            }
        }
    }
}

sealed class MediaSelectionResult {
    object NullMedia : MediaSelectionResult()
    data class AttachMediaError(val exception: Exception) : MediaSelectionResult()
    data class AttachMediaSuccess(val file: InternalFile) : MediaSelectionResult()
    object InvalidMedia : MediaSelectionResult()
}
