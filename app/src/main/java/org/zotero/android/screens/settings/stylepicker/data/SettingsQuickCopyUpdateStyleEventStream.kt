package org.zotero.android.screens.settings.stylepicker.data

import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.styles.data.Style
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsQuickCopyUpdateStyleEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Style>(applicationScope)