package org.zotero.android.pdf.reader.sidebar.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailPreviewCacheUpdatedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Int>(applicationScope)