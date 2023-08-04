package org.zotero.android.architecture.logging

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugLoggingDialogDataEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<DebugLoggingDialogData>(applicationScope)