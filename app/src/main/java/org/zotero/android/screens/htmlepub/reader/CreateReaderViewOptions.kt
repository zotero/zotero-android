package org.zotero.android.screens.htmlepub.reader

import com.google.gson.JsonArray

data class CreateReaderViewOptions(
    val type: String,
    val url: String,
    val annotations: JsonArray,
    var location: CreateReaderLocation? = null,
    var viewState: CreateReaderViewState? = null,
)

data class CreateReaderLocation(
    val annotationID: String,
)

data class CreateReaderViewState(
    //html
    val scrollYPercent: Double? = null,
    val scale: Double? = null,
    //epub
    val cfi: String? = null,
)