package org.zotero.android.screens.htmlepub.reader.search.data

import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubReaderSearchTermEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<HtmlEpubReaderSearchTermData>(applicationScope, HtmlEpubReaderSearchTermData(""))

data class HtmlEpubReaderSearchTermData(val term: String)