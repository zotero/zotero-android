package org.zotero.android.screens.htmlepub.reader.search.data

import com.google.gson.JsonObject
import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubReaderSearchResultsEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<HtmlEpubReaderSearchResultsData>(applicationScope, HtmlEpubReaderSearchResultsData(null))

data class HtmlEpubReaderSearchResultsData(val data: JsonObject?)