package org.zotero.android.websocket

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangeWsResponseKindEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<ChangeWsResponse.Kind>(applicationScope)