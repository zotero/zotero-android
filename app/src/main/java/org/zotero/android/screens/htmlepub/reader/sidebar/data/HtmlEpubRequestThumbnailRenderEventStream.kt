package org.zotero.android.screens.htmlepub.reader.sidebar.data

import dagger.hilt.android.scopes.ViewModelScoped
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject

@ViewModelScoped
class HtmlEpubRequestThumbnailRenderEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<List<Int>>(applicationScope)