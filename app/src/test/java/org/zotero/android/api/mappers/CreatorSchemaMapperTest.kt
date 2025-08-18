package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.zotero.android.utils.TestFilesUtils

class CreatorSchemaMapperTest {

    private val sut = CreatorSchemaMapper()

    @Test
    fun testCreatorSchemaWithCreatorTypeParsesSuccessfully() {
        runBlocking {
            val sampleJsonFile = TestFilesUtils.loadTextFile("CreatorSchemaWithCreatorTypeTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldNotBeNull()
            parseResult.creatorType shouldBeEqualTo "performer"
            parseResult.primary.shouldBeTrue()
        }
    }

    @Test
    fun testCreatorSchemaWithoutCreatorTypeReturnsNull() {
        runBlocking {
            val sampleJsonFile =
                TestFilesUtils.loadTextFile("CreatorSchemaWithoutCreatorTypeTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldBeNull()
        }
    }
}