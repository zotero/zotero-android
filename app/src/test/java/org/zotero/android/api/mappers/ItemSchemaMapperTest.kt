package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.zotero.android.api.pojo.sync.CreatorSchema
import org.zotero.android.api.pojo.sync.FieldSchema
import org.zotero.android.utils.TestFilesUtils

class ItemSchemaMapperTest {
    private val fieldSchemaMapper: FieldSchemaMapper = FieldSchemaMapper()
    private val creatorSchemaMapper: CreatorSchemaMapper = CreatorSchemaMapper()
    private val sut = ItemSchemaMapper(fieldSchemaMapper, creatorSchemaMapper)

    @Test
    fun testItemSchemaWithItemTypeParsesSuccessfully() {
        runBlocking {
            val sampleJsonFile =
                TestFilesUtils.loadTextFile("ItemSchemaWithItemTypeTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldNotBeNull()
            parseResult.itemType shouldBeEqualTo "artwork"
            parseResult.fields[0] shouldBeEqualTo FieldSchema("title", null)
            parseResult.creatorTypes[0] shouldBeEqualTo CreatorSchema("artist", true)
        }
    }

    @Test
    fun testItemSchemaWithoutItemTypeReturnsNull() {
        runBlocking {
            val sampleJsonFile =
                TestFilesUtils.loadTextFile("ItemSchemaWithoutItemTypeTestSample.json")
            val resultJsonObject =
                Gson().fromJson(sampleJsonFile, JsonObject::class.java).asJsonObject
            val parseResult = sut.fromJson(resultJsonObject)
            parseResult.shouldBeNull()
        }
    }
}