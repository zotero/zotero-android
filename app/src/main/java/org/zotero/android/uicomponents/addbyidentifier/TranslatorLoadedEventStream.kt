package org.zotero.android.uicomponents.addbyidentifier

import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatorLoadedEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<Boolean>(applicationScope = applicationScope, initValue = false)