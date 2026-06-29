package org.zotero.android.screens.reader.web.data

import com.google.gson.JsonArray

data class CreateReaderViewOptions(
    val type: String,
    val url: String,
    val annotations: JsonArray,
    var location: CreateReaderLocation? = null,
    var viewState: CreateReaderViewState = CreateReaderViewState(),

    var colorScheme: String = "light",
)

data class CreateReaderLocation(
    val annotationID: String,
)

data class CreateReaderViewState(
    //html
    var scrollYPercent: Double? = null,
    var scale: Double? = null,
    //epub
    var cfi: String? = null,

    //pdf
    var pageIndex: Int? = null,

    var flowMode: String? = null,
    var spreadMode: Int? = null,

)