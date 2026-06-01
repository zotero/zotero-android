package org.zotero.android.screens.htmlepub.reader.web

import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebData
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by multiple ViewModels
@Singleton
class HtmlEpubReaderWebCallChainEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Result<HtmlEpubReaderWebData>>(applicationScope)