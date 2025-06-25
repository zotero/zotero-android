package org.zotero.android.screens.settings.csllocalepicker.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsQuickCopyUpdateCslLocaleEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<ExportLocale>(applicationScope)