package org.zotero.android.architecture.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * A CoroutineScope used to run things for the life of the whole application.
 * Inject this into your component instead of using GlobalScope for better
 * testability.
 */
class ApplicationScope(
    override val coroutineContext: CoroutineContext,
) : CoroutineScope
