package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache

import android.graphics.Bitmap
import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewCacheSnapshotEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<ImmutableList<Bitmap?>>(applicationScope)