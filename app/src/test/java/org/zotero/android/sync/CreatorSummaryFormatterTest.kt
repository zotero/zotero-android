package org.zotero.android.sync

import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.ZoteroApplication
import org.zotero.android.database.objects.CreatorTypes
import org.zotero.android.database.objects.RCreator
import java.util.UUID

class CreatorSummaryFormatterTest {

    private val sut = CreatorSummaryFormatter

    @Before
    fun setUp() {
        mockkObject(ZoteroApplication)
        val mockZoteroApplication = mockk<ZoteroApplication>()
        ZoteroApplication.instance = mockZoteroApplication
    }

    @Test
    fun `returns null if creators list is empty`() {
        runBlocking {
            val allCreators = emptyList<RCreator>()

            val result = sut.summary(allCreators)
            result shouldBeEqualTo null
        }
    }

    @Test
    fun `if creators list contains primary creator returns summary of it's name`() {
        runBlocking {
            val creator1 = RCreator()
            creator1.uuid = UUID.randomUUID().toString()
            creator1.rawType = CreatorTypes.author
            creator1.firstName = "testFirstName1"
            creator1.lastName = "testLastName1"
            creator1.name = "testName1"
            creator1.orderId = 1
            creator1.primary = true

            val creator2 = RCreator()
            creator2.uuid = UUID.randomUUID().toString()
            creator2.rawType = CreatorTypes.contributor
            creator2.firstName = "testFirstName2"
            creator2.lastName = "testLastName2"
            creator2.name = "testName2"
            creator2.orderId = 2
            creator2.primary = false

            val creatorsList = listOf(creator1, creator2)

            val result = sut.summary(creatorsList)
            result shouldBeEqualTo creator1.summaryName
        }
    }

    @Test
    fun `if creators list contains no primary creator but contains editor return summary of it's name`() {
        runBlocking {
            val creator1 = RCreator()
            creator1.uuid = UUID.randomUUID().toString()
            creator1.rawType = CreatorTypes.author
            creator1.firstName = "testFirstName1"
            creator1.lastName = "testLastName1"
            creator1.name = "testName1"
            creator1.orderId = 1
            creator1.primary = false

            val creator2 = RCreator()
            creator2.uuid = UUID.randomUUID().toString()
            creator2.rawType = CreatorTypes.editor
            creator2.firstName = "testFirstName2"
            creator2.lastName = "testLastName2"
            creator2.name = "testName2"
            creator2.orderId = 2
            creator2.primary = false

            val creatorsList = listOf(creator1, creator2)

            val result = sut.summary(creatorsList)
            result shouldBeEqualTo creator2.summaryName
        }
    }

    @Test
    fun `if creators list contains no primary creator but contains contributor return summary of it's name`() {
        runBlocking {
            val creator1 = RCreator()
            creator1.uuid = UUID.randomUUID().toString()
            creator1.rawType = CreatorTypes.author
            creator1.firstName = "testFirstName1"
            creator1.lastName = "testLastName1"
            creator1.name = "testName1"
            creator1.orderId = 1
            creator1.primary = false

            val creator2 = RCreator()
            creator2.uuid = UUID.randomUUID().toString()
            creator2.rawType = CreatorTypes.contributor
            creator2.firstName = "testFirstName2"
            creator2.lastName = "testLastName2"
            creator2.name = "testName2"
            creator2.orderId = 2
            creator2.primary = false

            val creatorsList = listOf(creator1, creator2)

            val result = sut.summary(creatorsList)
            result shouldBeEqualTo creator2.summaryName
        }
    }
}