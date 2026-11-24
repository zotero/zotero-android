package org.zotero.android.screens.addnote.web

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.translator.data.WebPortResponse

internal sealed class NoteEditorMessage {
    object Initialized : NoteEditorMessage()

    object EditorStarted : NoteEditorMessage()

    data class Update(val value: String?) : NoteEditorMessage()

    data class Log(val text: String) : NoteEditorMessage()

    data class Unknown(val action: String?) : NoteEditorMessage()

    companion object {
        private const val MESSAGE_HANDLER = "messageHandler"
        private const val LOG_HANDLER = "logHandler"

        fun parse(gson: Gson, data: String): NoteEditorMessage {
            return try {
                val response = gson.fromJson(data, WebPortResponse::class.java)
                val message = response.message
                when (response.handlerName) {
                    LOG_HANDLER -> Log(if (message.isJsonPrimitive) message.asString else message.toString())
                    MESSAGE_HANDLER -> parseAction(message.asJsonObject)
                    else -> Unknown(action = null)
                }
            } catch (e: Exception) {
                Unknown(action = null)
            }
        }

        private fun parseAction(body: JsonObject): NoteEditorMessage {
            val action = body.stringOrNull("action")
            val parsed = when (action) {
                "initialized" -> Initialized
                "readerInitialized" -> EditorStarted
                "update" -> Update(value = body.stringOrNull("value"))
                else -> null
            }
            return parsed ?: Unknown(action)
        }

        private fun JsonObject.stringOrNull(name: String): String? {
            val element = get(name) ?: return null
            return if (element.isJsonPrimitive) element.asString else null
        }
    }
}