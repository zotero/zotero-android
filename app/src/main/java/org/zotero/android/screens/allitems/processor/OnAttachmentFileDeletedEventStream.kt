package org.zotero.android.screens.allitems.processor

import jakarta.inject.Inject
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope

class OnAttachmentFileDeletedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<EventBusConstants.AttachmentFileDeleted>(applicationScope)