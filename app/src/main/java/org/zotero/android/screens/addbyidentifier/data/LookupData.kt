package org.zotero.android.screens.addbyidentifier.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject

sealed interface LookupData {
    data class identifiers(val rawData: JsonArray) : LookupData
    data class item(val rawData: JsonObject) : LookupData
}
