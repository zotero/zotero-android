package org.zotero.android.architecture.logging.crash

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, as its being used by ZoteroApplication Class
@Singleton
class CrashShareDataEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<CrashReportIdDialogData>(applicationScope)