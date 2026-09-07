package org.zotero.android.screens.reader.search.data

import com.google.gson.JsonObject
import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by multiple ViewModels
@Singleton
class ReaderSearchResultsEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<ReaderSearchResultsData>(applicationScope, ReaderSearchResultsData(null))

data class ReaderSearchResultsData(val data: JsonObject?)