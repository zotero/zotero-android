package org.zotero.android.citation

import dagger.hilt.android.scopes.ViewModelScoped
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject

@ViewModelScoped
class CitationControllerPreviewHeightUpdateEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Int>(applicationScope)