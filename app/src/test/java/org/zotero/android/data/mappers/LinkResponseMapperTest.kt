package org.zotero.android.data.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.utils.TestFilesUtils

class LinkResponseMapperTest {

    private val sut = LinkResponseMapper()

    private lateinit var resultJsonObject: JsonObject

    @Before
    fun setUp() {
        val sampleJsonFile = TestFilesUtils.loadTextFile("LinkResponseTestSample.json")
        resultJsonObject = Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
    }

    @Test
    fun testMapperParsesSuccessfully() {
        runBlocking {
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult shouldBeEqualTo LinkResponse(
                href = "https://www.zotero.org/test_account",
                type = "text/html",
                title = "Test Title",
                length = 152
            )
        }
    }
}