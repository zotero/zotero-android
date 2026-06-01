package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache

import android.graphics.Bitmap
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject

@ViewModelScoped
class HtmlEpubThumbnailPreviewCacheSnapshotEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<ImmutableList<Bitmap?>>(applicationScope)