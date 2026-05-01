package org.zotero.android.screens.htmlepub.reader.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject

sealed interface HtmlEpubReaderWebData {
    object loadDocument : HtmlEpubReaderWebData
    data class saveAnnotations(val params: JsonObject) : HtmlEpubReaderWebData
    data class selectAnnotationFromDocument(val key: String): HtmlEpubReaderWebData
    object deselectSelectedAnnotation : HtmlEpubReaderWebData
    data class setSelectedTextParams(val params: JsonObject) : HtmlEpubReaderWebData
    data class setViewState(val params: JsonObject) : HtmlEpubReaderWebData
    data class showUrl(val url: String) : HtmlEpubReaderWebData

    data class parseOutline(val params: JsonObject) : HtmlEpubReaderWebData
    data class processDocumentSearchResults(val params: JsonObject) : HtmlEpubReaderWebData
    object toggleInterfaceVisibility : HtmlEpubReaderWebData

    data class onInitThumbnails(val thumbnailsJsonArray: JsonArray) : HtmlEpubReaderWebData
    data class onRenderThumbnail(val thumbnailJsonObject: JsonObject) : HtmlEpubReaderWebData
    data class onSetPageLabels(val pageLabelsJsonArray: JsonArray) : HtmlEpubReaderWebData
}