package org.zotero.android.screens.htmlepub.reader.data

import com.google.gson.JsonArray


data class DocumentUpdate(
    val deletions: JsonArray,
    val insertions: JsonArray,
    val modifications: JsonArray,
)