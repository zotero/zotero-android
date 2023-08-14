package org.zotero.android.sync

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncProgressHandler @Inject constructor(
    private val context: Context,
    private val dispatchers: Dispatchers
) {
    private var mainUiScope = CoroutineScope(dispatchers.main)

    fun reportFinish(errors: List<SyncError.NonFatal>) {
        mainUiScope.launch {
            val toast = if (errors.isEmpty()) {
                context.getString(Strings.finished_sync)
            } else {
                context.resources.getQuantityString(
                    Plurals.sync_toolbar_finished_sync_multiple_errors,
                    errors.size,
                    errors.size
                )
            }
            Toast.makeText(context, toast, LENGTH_LONG).show()
        }
    }

    fun reportAbort(error: Exception) {
        mainUiScope.launch {
            if(error is SyncError.NonFatal.schema) {
                val innerString = context.getString(Strings.sync_error_upgrade_required)
                val result = context.getString(Strings.sync_error_sync_failed, innerString)
                Toast.makeText(context, result, LENGTH_LONG).show()

            }
        }
    }
}