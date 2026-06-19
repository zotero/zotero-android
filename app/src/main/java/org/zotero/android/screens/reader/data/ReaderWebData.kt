package org.zotero.android.screens.reader.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject

sealed interface ReaderWebData {
    object loadDocument : ReaderWebData
    data class saveAnnotations(val params: JsonObject) : ReaderWebData
    data class selectAnnotationFromDocument(val key: String): ReaderWebData
    object deselectSelectedAnnotation : ReaderWebData
    data class setSelectedTextParams(val params: JsonObject) : ReaderWebData
    data class setViewState(val params: JsonObject) : ReaderWebData
    data class setViewStats(val params: JsonObject) : ReaderWebData
    data class showUrl(val url: String) : ReaderWebData

    data class parseOutline(val params: JsonObject) : ReaderWebData
    data class processDocumentSearchResults(val params: JsonObject) : ReaderWebData
    object toggleInterfaceVisibility : ReaderWebData

    data class onInitThumbnails(val thumbnailsJsonArray: JsonArray) : ReaderWebData
    data class onRenderThumbnail(val thumbnailJsonObject: JsonObject) : ReaderWebData
    data class onSetPageLabels(val pageLabelsJsonArray: JsonArray) : ReaderWebData
}