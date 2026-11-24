package org.zotero.android.screens.addnote.web

import com.google.gson.Gson
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class NoteEditorMessageTest {

    private val gson = Gson()

    private fun parse(json: String) = NoteEditorMessage.parse(gson, json)

    private fun messageHandler(body: String) = """{"handlerName":"messageHandler","message":$body}"""

    @Test
    fun `initialized message`() {
        parse(messageHandler("""{"action":"initialized"}""")) shouldBeEqualTo NoteEditorMessage.Initialized
    }

    @Test
    fun `editor started message`() {
        parse(messageHandler("""{"action":"readerInitialized"}""")) shouldBeEqualTo NoteEditorMessage.EditorStarted
    }

    @Test
    fun `update message carries the note html`() {
        parse(messageHandler("""{"action":"update","value":"<div data-schema-version=\"9\"><p>Hi \"there\"</p>\n</div>"}""")) shouldBeEqualTo
                NoteEditorMessage.Update(value = "<div data-schema-version=\"9\"><p>Hi \"there\"</p>\n</div>")
    }

    @Test
    fun `update message with null value is what forceSave answers when nothing changed`() {
        parse(messageHandler("""{"action":"update","value":null}""")) shouldBeEqualTo NoteEditorMessage.Update(value = null)
    }

    @Test
    fun `update message without a value`() {
        parse(messageHandler("""{"action":"update"}""")) shouldBeEqualTo NoteEditorMessage.Update(value = null)
    }

    @Test
    fun `empty note is an empty string and not null`() {
        parse(messageHandler("""{"action":"update","value":""}""")) shouldBeEqualTo NoteEditorMessage.Update(value = "")
    }

    @Test
    fun `log message`() {
        parse("""{"handlerName":"logHandler","message":"Notify subscription: abc"}""") shouldBeEqualTo
                NoteEditorMessage.Log(text = "Notify subscription: abc")
    }

    @Test
    fun `actions that are not handled are reported as unknown`() {
        parse(messageHandler("""{"action":"showNote"}""")) shouldBeEqualTo NoteEditorMessage.Unknown(action = "showNote")
    }

    @Test
    fun `message without an action is unknown`() {
        parse(messageHandler("""{"value":"x"}""")) shouldBeEqualTo NoteEditorMessage.Unknown(action = null)
    }

    @Test
    fun `unknown handler`() {
        parse("""{"handlerName":"somethingElse","message":{"action":"update","value":"x"}}""") shouldBeEqualTo
                NoteEditorMessage.Unknown(action = null)
    }

    @Test
    fun `message that is not an object does not crash`() {
        parse(messageHandler(""""initialized"""")) shouldBeEqualTo NoteEditorMessage.Unknown(action = null)
    }

    @Test
    fun `malformed json does not crash`() {
        parse("not json") shouldBeEqualTo NoteEditorMessage.Unknown(action = null)
        parse("") shouldBeEqualTo NoteEditorMessage.Unknown(action = null)
        parse("""{"message":{"action":"initialized"}}""") shouldBeEqualTo NoteEditorMessage.Unknown(action = null)
    }
}