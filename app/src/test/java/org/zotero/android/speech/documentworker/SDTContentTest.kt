package org.zotero.android.speech.documentworker

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class SDTContentTest {

    @Test
    fun `partChain follows previous parts to the root and stops at the first excluded part`() {
        val structure = document(
            paragraph("one", nextPart = listOf(1)),
            paragraph("two", previousPart = listOf(0), nextPart = listOf(2)),
            JsonObject().apply {
                addProperty("type", "paragraph")
                addProperty("flowClass", "excluded")
                add("previousPart", JsonArray().apply { add(1) })
                add("content", JsonArray().apply { add(JsonObject().apply { addProperty("text", "three") }) })
            }
        )

        val chain = SDTContent.partChain(structure, listOf(1), include = { _, node -> node.get("flowClass")?.asString != "excluded" })

        chain.map { it.ref } shouldBeEqualTo listOf(listOf(0), listOf(1))
    }

    @Test
    fun `partBoundarySeparator separates words with a space and nothing else`() {
        separator("end", "start") shouldBeEqualTo " "
        separator("end.", "start") shouldBeEqualTo ""
        separator("end ", "start") shouldBeEqualTo ""
        separator("end", " start") shouldBeEqualTo ""
        separator("число", "δέκα") shouldBeEqualTo " "
        separator("42", "7") shouldBeEqualTo " "
    }

    private fun separator(previous: String, next: String): String {
        return SDTContent.partBoundarySeparator(paragraph(previous), paragraph(next))
    }

    private fun document(vararg content: JsonObject): JsonObject {
        return JsonObject().apply {
            add("catalog", JsonObject().apply { add("pages", JsonArray()) })
            add("content", JsonArray().apply { content.forEach { add(it) } })
        }
    }

    private fun paragraph(text: String, previousPart: List<Int>? = null, nextPart: List<Int>? = null): JsonObject {
        return JsonObject().apply {
            addProperty("type", "paragraph")
            add("content", JsonArray().apply { add(JsonObject().apply { addProperty("text", text) }) })
            if (previousPart != null) {
                add("previousPart", JsonArray().apply { previousPart.forEach { add(it) } })
            }
            if (nextPart != null) {
                add("nextPart", JsonArray().apply { nextPart.forEach { add(it) } })
            }
        }
    }
}