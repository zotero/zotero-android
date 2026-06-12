package org.zotero.android.screens.reader.web

import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.screens.reader.data.ReaderWebData
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by multiple ViewModels
@Singleton
class ReaderWebCallChainEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Result<ReaderWebData>>(applicationScope)