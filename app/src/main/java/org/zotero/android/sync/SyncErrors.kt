package org.zotero.android.sync

sealed class SyncError {
    class ErrorData(
        val itemKeys: List<String>?,
        val libraryId: LibraryIdentifier
    ) {
        companion object {
            fun from(libraryId: LibraryIdentifier): ErrorData {
                return ErrorData(itemKeys = null, libraryId = libraryId)
            }

            fun from(
                syncObject: SyncObject,
                keys: List<String>,
                libraryId: LibraryIdentifier
            ): ErrorData {
                return when (syncObject) {
                    SyncObject.item ->
                        ErrorData(itemKeys = keys, libraryId = libraryId)
                    SyncObject.collection, SyncObject.search, SyncObject.settings, SyncObject.trash ->
                        ErrorData(itemKeys = null, libraryId = libraryId)
                }
            }
        }

    }

    data class fatal2(val error: Fatal) : SyncError()
    data class nonFatal2(val error: NonFatal) : SyncError()

    val fatal2S: Fatal?
        get() {
            when (this) {
                is SyncError.fatal2 -> return error
                is SyncError.nonFatal2 -> return null
            }
        }

    val nonFatal2S: NonFatal?
        get() {
            return when (this) {
                is SyncError.fatal2 -> null
                is SyncError.nonFatal2 -> error
            }
        }

    sealed class Fatal : Exception() {
        object noInternetConnection : Fatal()
        data class apiError(val response: String, val data: ErrorData) : Fatal()
        object dbError : Fatal()
        object groupSyncFailed : Fatal()
        object allLibrariesFetchFailed : Fatal()
        data class uploadObjectConflict(val data: ErrorData) : Fatal()
        data class cantSubmitAttachmentItem(val data: ErrorData): Fatal()
        object permissionLoadingFailed : Fatal()
        object missingGroupPermissions : Fatal()
        object cancelled : Fatal()
        object serviceUnavailable : Fatal()
        object forbidden : Fatal()


        override fun equals(other: Any?): Boolean {
            if (other == null) {
                return false
            }
            val nFOther = other as Fatal
            return when {
                (this is noInternetConnection && nFOther is noInternetConnection)
                        || (this is apiError && nFOther is apiError)
                        || (this is dbError && nFOther is dbError)
                        || (this is groupSyncFailed && nFOther is groupSyncFailed)
                        || (this is allLibrariesFetchFailed && nFOther is allLibrariesFetchFailed)
                        || (this is cancelled && nFOther is cancelled)


                -> {
                    true
                }
                else -> false
            }
        }
    }

    sealed class NonFatal : Exception() {
        data class versionMismatch(val libraryId: LibraryIdentifier) : NonFatal()
        data class apiError(val response: String, val data: ErrorData) : NonFatal()
        data class unknown(val str: String, val data: ErrorData) : NonFatal()
        data class schema(val error: SchemaError, val data: ErrorData) : NonFatal()
        data class parsing(val error: Parsing.Error, val data: ErrorData) : NonFatal()
        data class quotaLimit(val libraryId: LibraryIdentifier) : NonFatal()
        object unchanged : NonFatal()
        data class attachmentMissing(
            val key: String,
            val libraryId: LibraryIdentifier,
            val title: String
        ) : NonFatal()

        data class annotationDidSplit(val messageS: String, val keys: Set<String>,val libraryId: LibraryIdentifier) :
            NonFatal()

        object insufficientSpace : NonFatal()
        data class webDavDeletion(val count: Int, val library: String) : NonFatal()
        data class webDavDeletionFailed(val error: String, val library: String) : NonFatal()
        data class preconditionFailed(val libraryId: LibraryIdentifier): NonFatal()

        val isVersionMismatch: Boolean
            get() {
                when (this) {
                    is versionMismatch ->
                        return true
                    else ->
                        return false
                }
            }

        override fun equals(other: Any?): Boolean {
            if (other == null) {
                return false
            }
            val nFOther = other as NonFatal
            return when {
                (this is versionMismatch && nFOther is versionMismatch) -> {
                    true
                }
                else -> false
            }
        }
    }
}

sealed class SyncActionError : Exception() {
    data class annotationNeededSplitting(val messageS: String, val keys: Set<String>, val libraryId: LibraryIdentifier): SyncActionError()
    object attachmentAlreadyUploaded: SyncActionError()
    object attachmentItemNotSubmitted: SyncActionError()
    data class attachmentMissing(val key: String, val libraryId: LibraryIdentifier, val title: String): SyncActionError()
    data class authorizationFailed(val statusCode: Int, val response: String, val hadIfMatchHeader: Boolean): SyncActionError()
    object objectPreconditionError: SyncActionError()
    data class submitUpdateFailures(val messageS: String): SyncActionError()
}

