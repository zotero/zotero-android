package org.zotero.android.attachmentdownloader

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAttachmentDownloaderEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<RemoteAttachmentDownloader.Update>(applicationScope)