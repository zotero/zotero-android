package org.zotero.android.pdf.cache

import dagger.hilt.android.scopes.ViewModelScoped
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject

@ViewModelScoped
class AnnotationPreviewCacheUpdatedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<String>(applicationScope)