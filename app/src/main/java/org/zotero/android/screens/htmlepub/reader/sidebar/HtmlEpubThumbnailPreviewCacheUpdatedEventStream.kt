package org.zotero.android.screens.htmlepub.reader.sidebar

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewCacheUpdatedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Int>(applicationScope)