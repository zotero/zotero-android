package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RLastReadDate
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.requests.CountObjectsDbRequest
import org.zotero.android.database.requests.PerformCollectionDeletionsDbRequest
import org.zotero.android.database.requests.PerformItemDeletionsDbRequest
import org.zotero.android.database.requests.PerformLastReadDeletionsDbRequest
import org.zotero.android.database.requests.PerformPageIndexDeletionsDbRequest
import org.zotero.android.database.requests.PerformSearchDeletionsDbRequest
import org.zotero.android.database.requests.PerformTagDeletionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import kotlin.math.min


data class PerformDeletionsSyncActionResult(
    val conflicts: List<Pair<String, String>>,
    val unexpectedMyLibraryLastReadDeletions: List<String>
)

class PerformDeletionsSyncAction @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("collections") private val collections: List<String>,
    @Assisted("items") private val items: List<String>,
    @Assisted("searches") private val searches: List<String>,
    @Assisted("tags") private val tags: List<String>,
    @Assisted("settings") private val settings: List<String>,
    @Assisted("conflictMode") private val conflictMode: PerformItemDeletionsDbRequest.ConflictResolutionMode,

    private val dbWrapperMain: DbWrapperMain,
) {
    private val batchSize = 500

    fun result(): PerformDeletionsSyncActionResult {
        val hasCollections = dbWrapperMain.realmDbStorage.perform(
            request = CountObjectsDbRequest(
                RCollection::class
            )
        ) > 0
        if (hasCollections) {
            batch(values = collections, batchSize = this.batchSize) { batch ->
                dbWrapperMain.realmDbStorage.perform(
                    request = PerformCollectionDeletionsDbRequest(
                        libraryId = libraryId,
                        keys = batch
                    )
                )
            }
        }

        val hasSearches =
            dbWrapperMain.realmDbStorage.perform(request = CountObjectsDbRequest(RSearch::class)) > 0
        if (hasSearches) {
            batch(values = searches, batchSize = this.batchSize) { batch ->
                dbWrapperMain.realmDbStorage.perform(
                    request = PerformSearchDeletionsDbRequest(
                        libraryId = libraryId,
                        keys = batch
                    )
                )
            }
        }

        val hasTags =
            dbWrapperMain.realmDbStorage.perform(request = CountObjectsDbRequest(RTag::class)) > 0
        if (hasTags) {
            batch(values = tags, batchSize = this.batchSize) { batch ->
                dbWrapperMain.realmDbStorage.perform(
                    request = PerformTagDeletionsDbRequest(
                        libraryId = libraryId,
                        names = batch
                    )
                )
            }
        }

        val conflicts: MutableList<Pair<String, String>> = mutableListOf()
        val hasItems = dbWrapperMain.realmDbStorage.perform(
            request = CountObjectsDbRequest(
                RItem::class
            )
        ) > 0
        if (hasItems) {
            batch(values = items, batchSize = this.batchSize) { batch ->
                val batchConflicts = dbWrapperMain.realmDbStorage.perform(
                    request = PerformItemDeletionsDbRequest(
                        libraryId = libraryId,
                        keys = batch,
                        conflictMode = conflictMode
                    )
                )
                conflicts.addAll(batchConflicts)
            }
        }

        val pageIndices = settings.filter { it.startsWith("lastPageIndex_") }
        val hasPageIndices =
            dbWrapperMain.realmDbStorage.perform(request = CountObjectsDbRequest(RPageIndex::class)) > 0
        if (hasPageIndices) {
            batch(values = pageIndices, batchSize = this.batchSize) { uids ->
                val groupedIndices = mutableMapOf<LibraryIdentifier, MutableList<String>>()
                for (uid in uids) {
                    val (key, libraryId) = SettingKeyParser.parse(key = uid)
                    groupedIndices.getOrPut(libraryId, { mutableListOf() }).add(key)
                }
                for ((libraryId, keys) in groupedIndices) {
                    dbWrapperMain.realmDbStorage.perform(
                        request = PerformPageIndexDeletionsDbRequest(
                            libraryId = libraryId,
                            keys = keys
                        )
                    )
                }
            }
        }

        val unexpectedMyLibraryLastReadDeletions = mutableListOf<String>()
        val lastRead = settings.filter { it.startsWith("lastRead_") }
        val hasLastRead =
            dbWrapperMain.realmDbStorage.perform(request = CountObjectsDbRequest(RLastReadDate::class)) > 0
        if (hasLastRead) {
            batch(values = lastRead, batchSize = this.batchSize) { uids ->
                val groupedIndices = mutableMapOf<LibraryIdentifier, MutableList<String>>()
                for (uid in uids) {
                    val (key, libraryId) = SettingKeyParser.parse(key = uid)
                    groupedIndices.getOrPut(libraryId, { mutableListOf() }).add(key)
                }
                for ((libraryId, keys) in groupedIndices) {
                    try {
                        dbWrapperMain.realmDbStorage.perform(
                            request = PerformLastReadDeletionsDbRequest(
                                libraryId = libraryId,
                                keys = keys
                            )
                        )
                    } catch (error: Exception) {
                        when (error) {
                            is PerformLastReadDeletionsDbRequest.Error.myLibraryNotSupported -> {
                                unexpectedMyLibraryLastReadDeletions.addAll(keys)
                            }

                            else -> {
                                throw error
                            }
                        }
                    }
                }
            }
        }
        if (!unexpectedMyLibraryLastReadDeletions.isEmpty()) {
            Timber.w("PerformDeletionsSyncAction: Received unexpected My Library lastRead deletions - $unexpectedMyLibraryLastReadDeletions")
        }

        return PerformDeletionsSyncActionResult(
            conflicts = conflicts,
            unexpectedMyLibraryLastReadDeletions = unexpectedMyLibraryLastReadDeletions
        )
    }


    private fun batch(values: List<String>, batchSize: Int, deleteValues: (List<String>) -> Unit) {
        if (values.isEmpty()) {
            return
        }
        var count = 0
        while (count < values.size) {
            val upperLimit = min(count + batchSize, values.size)
            val slice = values.slice(count..<upperLimit)
            deleteValues(slice)
            count = upperLimit
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("collections") collections: List<String>,
            @Assisted("items") items: List<String>,
            @Assisted("searches") searches: List<String>,
            @Assisted("tags") tags: List<String>,
            @Assisted("settings") settings: List<String>,
            @Assisted("conflictMode") conflictMode: PerformItemDeletionsDbRequest.ConflictResolutionMode
        ): PerformDeletionsSyncAction
    }

}