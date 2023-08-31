package org.zotero.android.architecture.navigation.toolbar.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncProgressEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<SyncProgress>(applicationScope)