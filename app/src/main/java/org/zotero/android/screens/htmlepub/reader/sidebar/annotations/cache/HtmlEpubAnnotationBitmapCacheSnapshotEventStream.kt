package org.zotero.android.screens.htmlepub.reader.sidebar.annotations.cache

import android.graphics.Bitmap
import kotlinx.collections.immutable.PersistentMap
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubAnnotationBitmapCacheSnapshotEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<PersistentMap<String, Bitmap>>(applicationScope)