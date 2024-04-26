package org.zotero.android.uicomponents.addbyidentifier.data

import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LookupDataEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Result<LookupData>>(applicationScope)