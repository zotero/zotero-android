package org.zotero.android.data.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.LinkResponse
import org.zotero.android.api.pojo.sync.LinksResponse
import org.zotero.android.utils.TestFilesUtils

class LibraryResponseMapperTest {
    private val linkResponseMapper: LinkResponseMapper = LinkResponseMapper()
    private val linksResponseMapper: LinksResponseMapper = LinksResponseMapper(linkResponseMapper)
    private val sut = LibraryResponseMapper(linksResponseMapper)

    private lateinit var resultJsonObject: JsonObject

    @Before
    fun setUp() {
        val sampleJsonFile = TestFilesUtils.loadTextFile("LibraryResponseTestSample.json")
        resultJsonObject = Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
    }

    @Test
    fun testMapperParsesSuccessfully() {
        runBlocking {
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult shouldBeEqualTo LibraryResponse(
                id = 9886869,
                name = "testtestf",
                type = "user",
                links = LinksResponse(
                    itself = null,
                    alternate = LinkResponse(
                        href = "https://www.zotero.org/testtestf",
                        type = "text/html",
                        title = null,
                        length = null
                    ),
                    up = null,
                    enclosure = null
                )
            )
        }
    }
}