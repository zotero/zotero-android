package org.zotero.android.citation

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CitationControllerPreviewUpdateEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Pair<String, Boolean>>(applicationScope)