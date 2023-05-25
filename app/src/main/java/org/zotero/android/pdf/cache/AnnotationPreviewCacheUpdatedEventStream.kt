package org.zotero.android.pdf.cache

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationPreviewCacheUpdatedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<String>(applicationScope)