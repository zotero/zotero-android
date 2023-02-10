package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.zotero.android.api.pojo.sync.CreatorResponse
import org.zotero.android.utils.TestFilesUtils

class CreatorResponseMapperTest {
    private val sut = CreatorResponseMapper()

    private lateinit var resultJsonObject: JsonObject

    @Before
    fun setUp() {
        val sampleJsonFile = TestFilesUtils.loadTextFile("CreatorResponseTestSample.json")
        resultJsonObject = Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
    }

    @Test
    fun testMapperParsesSuccessfully() {
        runBlocking {
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult shouldBeEqualTo CreatorResponse(
                name = "Jason",
                firstName = "Jason",
                lastName = "Beckerman",
                creatorType = "podcaster"
            )
        }
    }
}