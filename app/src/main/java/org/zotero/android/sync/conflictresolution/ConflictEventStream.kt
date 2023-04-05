package org.zotero.android.sync.conflictresolution

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Conflict>(applicationScope)