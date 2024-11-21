package org.zotero.android.sync

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.database.objects.RCustomLibraryType
import java.util.UUID

class ActionsCreatorTest {

    private val sut = ActionsCreator()

    @Before
    fun setUp() {
    }

    @Test
    fun `createGroupActions for full SynkKind full and Libraries all should return these actions in order resolveDeletedGroup, syncGroupToDb and createLibraryAction with onlyDownloads`() {
        runBlocking {
            val updatedGroupId1 = 1
            val updatedGroupId2 = 2
            val updateIds = listOf(updatedGroupId1, updatedGroupId2)

            val deletedGroup1Id = 3
            val deletedGroup1Name = "Deleted Group 3"

            val deletedGroup2Id = 4
            val deletedGroup2Name = "Deleted Group 4"

            val deletedGroups = listOf(
                Pair(deletedGroup1Id, deletedGroup1Name),
                Pair(deletedGroup2Id, deletedGroup2Name)
            )

            val result = sut.createGroupActions(
                updateIds = updateIds,
                deleteGroups = deletedGroups,
                syncType = SyncKind.full,
                libraryType = Libraries.all
            )
            val expectedResult = listOf(
                Action.resolveDeletedGroup(groupId = deletedGroup1Id, name = deletedGroup1Name),
                Action.resolveDeletedGroup(groupId = deletedGroup2Id, name = deletedGroup2Name),
                Action.syncGroupToDb(groupId = updatedGroupId1),
                Action.syncGroupToDb(groupId = updatedGroupId2),
                Action.createLibraryActions(
                    librarySyncType = Libraries.all,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.onlyDownloads
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }


    @Test
    fun `createBatchedObjectActions for empty keys and shouldStoreVersion true should return list of storeVersions`() {
        runBlocking {
            val groupLibraryId = 1
            val libraryId = LibraryIdentifier.group(groupLibraryId)

            val objectS = SyncObject.item
            val keys = emptyList<String>()
            val version = 1
            val shouldStoreVersion = true

            val result = sut.createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = keys,
                version = version,
                shouldStoreVersion = shouldStoreVersion,
            )

            val expectedResult = listOf(
                Action.storeVersion(
                    version = version,
                    libraryId = libraryId,
                    syncObject = objectS
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createBatchedObjectActions for empty keys and shouldStoreVersion false should return list of emptyList`() {
        runBlocking {
            val groupLibraryId = 1
            val libraryId = LibraryIdentifier.group(groupLibraryId)

            val objectS = SyncObject.item
            val keys = emptyList<String>()
            val version = 1
            val shouldStoreVersion = false

            val result = sut.createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = keys,
                version = version,
                shouldStoreVersion = shouldStoreVersion,
            )

            val expectedResult = emptyList<Action>()
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createBatchedObjectActions for two keys and shouldStoreVersion true should return 1 batch of actions with storeVersion action`() {
        runBlocking {
            val groupLibraryId = 1
            val libraryId = LibraryIdentifier.group(groupLibraryId)

            val objectS = SyncObject.item
            val key1 = "key1"
            val key2 = "key2"
            val keys = listOf(key1, key2)
            val version = 1
            val shouldStoreVersion = true

            val result = sut.createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = keys,
                version = version,
                shouldStoreVersion = shouldStoreVersion,
            )

            val expectedResult = listOf(
                Action.syncBatchesToDb(
                    batches = listOf(
                        DownloadBatch(
                            libraryId = libraryId,
                            objectS = objectS,
                            keys = keys,
                            version = version
                        )
                    )
                ),
                Action.storeVersion(
                    version = version,
                    libraryId = libraryId,
                    syncObject = objectS
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createGroupActions for full SynkKind normal and Libraries all should return these actions in order resolveDeletedGroup, syncGroupToDb and createLibraryAction with automatic`() {
        runBlocking {
            val updatedGroupId1 = 1
            val updatedGroupId2 = 2
            val updateIds = listOf(updatedGroupId1, updatedGroupId2)

            val deletedGroup1Id = 3
            val deletedGroup1Name = "Deleted Group 3"

            val deletedGroup2Id = 4
            val deletedGroup2Name = "Deleted Group 4"

            val deletedGroups = listOf(
                Pair(deletedGroup1Id, deletedGroup1Name),
                Pair(deletedGroup2Id, deletedGroup2Name)
            )

            val result = sut.createGroupActions(
                updateIds = updateIds,
                deleteGroups = deletedGroups,
                syncType = SyncKind.normal,
                libraryType = Libraries.all
            )
            val expectedResult = listOf(
                Action.resolveDeletedGroup(groupId = deletedGroup1Id, name = deletedGroup1Name),
                Action.resolveDeletedGroup(groupId = deletedGroup2Id, name = deletedGroup2Name),
                Action.syncGroupToDb(groupId = updatedGroupId1),
                Action.syncGroupToDb(groupId = updatedGroupId2),
                Action.createLibraryActions(
                    librarySyncType = Libraries.all,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.automatic
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createBatchedObjectActions for 20 keys and shouldStoreVersion false should return two batches of actions`() {
        runBlocking {
            val groupLibraryId = 1
            val libraryId = LibraryIdentifier.group(groupLibraryId)

            val objectS = SyncObject.item
            val keys = mutableListOf<String>()
            repeat(20) {
                keys.add(UUID.randomUUID().toString())
            }
            val version = 1
            val shouldStoreVersion = false

            val result = sut.createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = keys,
                version = version,
                shouldStoreVersion = shouldStoreVersion,
            )

            val batch1Keys = keys.subList(0, 10)
            val batch2Keys = keys.subList(10, keys.size)

            val expectedResult = listOf(
                Action.syncBatchesToDb(
                    batches = listOf(
                        DownloadBatch(
                            libraryId = libraryId,
                            objectS = objectS,
                            keys = batch1Keys,
                            version = version
                        ),
                        DownloadBatch(
                            libraryId = libraryId,
                            objectS = objectS,
                            keys = batch2Keys,
                            version = version
                        )
                    )
                ),
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createInitialActions for Libraries_all should return Action_loadKeyPermissions and Action_syncGroupVersions`() {
        runBlocking {
            val libraries = Libraries.all
            val syncType = SyncKind.full

            val result = sut.createInitialActions(
                libraries = libraries,
                syncType = syncType
            )

            val expectedResult = listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createInitialActions for Libraries_specific with at least one group library should return Action_loadKeyPermissions and Action_syncGroupVersions`() {
        runBlocking {
            val customMyLibrary = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
            val groupIdentifier = LibraryIdentifier.group(1)
            val libraries = Libraries.specific(
                listOf(
                    customMyLibrary,
                    groupIdentifier
                )
            )
            val syncType = SyncKind.full

            val result = sut.createInitialActions(
                libraries = libraries,
                syncType = syncType
            )

            val expectedResult = listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createInitialActions for SyncType_full and list of Libraries_specific libraries should return Action_loadKeyPermissions and list of onlyDownloads createLibraryActions`() {
        runBlocking {
            val customLibrary1 = LibraryIdentifier.custom(mockk<RCustomLibraryType>())
            val customLibrary2 = LibraryIdentifier.custom(mockk<RCustomLibraryType>())
            val libraries = Libraries.specific(
                listOf(
                    customLibrary1,
                    customLibrary2
                )
            )
            val syncType = SyncKind.full

            val result = sut.createInitialActions(
                libraries = libraries,
                syncType = syncType
            )

            val expectedResult = listOf(
                Action.loadKeyPermissions,
                Action.createLibraryActions(
                    librarySyncType = libraries,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.onlyDownloads
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createGroupActions for full SynkKind prioritizeDownloads and Libraries all should return these actions in order resolveDeletedGroup, syncGroupToDb and createLibraryAction with onlyDownloads, onlyWrites`() {
        runBlocking {
            val updatedGroupId1 = 1
            val updatedGroupId2 = 2
            val updateIds = listOf(updatedGroupId1, updatedGroupId2)

            val deletedGroup1Id = 3
            val deletedGroup1Name = "Deleted Group 3"

            val deletedGroup2Id = 4
            val deletedGroup2Name = "Deleted Group 4"

            val deletedGroups = listOf(
                Pair(deletedGroup1Id, deletedGroup1Name),
                Pair(deletedGroup2Id, deletedGroup2Name)
            )

            val result = sut.createGroupActions(
                updateIds = updateIds,
                deleteGroups = deletedGroups,
                syncType = SyncKind.prioritizeDownloads,
                libraryType = Libraries.all
            )
            val expectedResult = listOf(
                Action.resolveDeletedGroup(groupId = deletedGroup1Id, name = deletedGroup1Name),
                Action.resolveDeletedGroup(groupId = deletedGroup2Id, name = deletedGroup2Name),
                Action.syncGroupToDb(groupId = updatedGroupId1),
                Action.syncGroupToDb(groupId = updatedGroupId2),
                Action.createLibraryActions(
                    librarySyncType = Libraries.all,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.onlyDownloads
                ),
                Action.createLibraryActions(
                    librarySyncType = Libraries.all,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.onlyWrites
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

    @Test
    fun `createInitialActions for SyncType_normal and list of Libraries_specific libraries should return Action_loadKeyPermissions and list of automatic createLibraryActions`() {
        runBlocking {
            val customLibrary1 = LibraryIdentifier.custom(mockk<RCustomLibraryType>())
            val customLibrary2 = LibraryIdentifier.custom(mockk<RCustomLibraryType>())
            val libraries = Libraries.specific(
                listOf(
                    customLibrary1,
                    customLibrary2
                )
            )
            val syncType = SyncKind.normal

            val result = sut.createInitialActions(
                libraries = libraries,
                syncType = syncType
            )

            val expectedResult = listOf(
                Action.loadKeyPermissions,
                Action.createLibraryActions(
                    librarySyncType = libraries,
                    createLibraryActionsOptions = CreateLibraryActionsOptions.automatic
                )
            )
            result shouldBeEqualTo expectedResult
        }
    }

}