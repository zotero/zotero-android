package org.zotero.android.screens.reader.search.data

import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by multiple ViewModels
@Singleton
class ReaderSearchTermEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<ReaderSearchTermData>(applicationScope, ReaderSearchTermData(""))

data class ReaderSearchTermData(val term: String)