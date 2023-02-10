package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import org.zotero.android.utils.TestFilesUtils

class LinksResponseMapperTest {

    private val linkResponseMapper = LinkResponseMapper()

    private val sut = LinksResponseMapper(linkResponseMapper)

    private lateinit var resultJsonObject: JsonObject

    @Before
    fun setUp() {
        val sampleJsonFile = TestFilesUtils.loadTextFile("LinksResponseTestSample.json")
        resultJsonObject = Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
    }

    @Test
    fun testMapperParsesSuccessfully() {
        runBlocking {
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult shouldBeEqualTo LinksResponse(
                itself = LinkResponse(
                    href = "https://api.zotero.org/users/9886869/items/QLFEU9EN",
                    type = "application/json",
                    title = null,
                    length = null
                ),
                alternate = LinkResponse(
                    href = "https://www.zotero.org/testtestf/items/QLFEU9EN",
                    type = "text/html",
                    title = null,
                    length = null
                ),
                up = null,
                enclosure = null
            )
        }
    }
}