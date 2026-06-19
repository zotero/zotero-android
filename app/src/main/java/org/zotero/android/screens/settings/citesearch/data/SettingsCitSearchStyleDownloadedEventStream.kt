package org.zotero.android.screens.settings.citesearch.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by multiple ViewModels
@Singleton
class SettingsCitSearchStyleDownloadedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Unit>(applicationScope)