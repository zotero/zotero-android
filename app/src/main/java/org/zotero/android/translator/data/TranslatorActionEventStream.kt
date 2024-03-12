package org.zotero.android.translator.data

import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatorActionEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<Result<TranslatorAction>>(applicationScope)