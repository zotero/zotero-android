package org.zotero.android.architecture.attachmentdownloader

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<AttachmentDownloader.Update>(applicationScope)