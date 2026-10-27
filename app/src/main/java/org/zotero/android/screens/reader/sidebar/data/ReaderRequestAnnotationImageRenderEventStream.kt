package org.zotero.android.screens.reader.sidebar.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRequestAnnotationImageRenderEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<List<String>>(applicationScope)