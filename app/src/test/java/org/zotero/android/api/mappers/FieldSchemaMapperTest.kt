package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.zotero.android.utils.TestFilesUtils

class FieldSchemaMapperTest {

    private val sut = FieldSchemaMapper()

    @Test
    fun testFieldSchemaWithFieldParsesSuccessfully() {
        runBlocking {
            val sampleJsonFile = TestFilesUtils.loadTextFile("FieldSchemaWithFieldTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldNotBeNull()
            parseResult.field shouldBeEqualTo "audioRecordingFormat"
            parseResult.baseField shouldBeEqualTo "medium"
        }
    }

    @Test
    fun testFieldSchemaWithoutFieldReturnsNull() {
        runBlocking {
            val sampleJsonFile =
                TestFilesUtils.loadTextFile("FieldSchemaWithoutFieldTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldBeNull()
        }
    }
}