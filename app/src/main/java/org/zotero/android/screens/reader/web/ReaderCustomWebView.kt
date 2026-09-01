package org.zotero.android.screens.reader.web

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.View
import android.webkit.WebView

class ReaderCustomWebView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

    var selectionActionModeDelegate: ReaderSelectionActionModeDelegate? = null

    private var activeSelectionActionMode: ActionMode? = null

    fun invalidateSelectionActionMode() {
        activeSelectionActionMode?.invalidate()
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback
    ): ActionMode {
        return super.startActionModeForChild(originalView, callback)
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode {
        return super.startActionModeForChild(originalView, callback, type)
    }

    public override fun startActionMode(callback: ActionMode.Callback): ActionMode {
        val actionMode = startActionModeForChild(this, wrapSelectionCallback(callback))
        activeSelectionActionMode = actionMode
        return actionMode
    }

    public override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode {
        val actionMode = startActionModeForChild(this, wrapSelectionCallback(callback), type)
        activeSelectionActionMode = actionMode
        return actionMode
    }

    private fun wrapSelectionCallback(callback: ActionMode.Callback): ActionMode.Callback {
        return ReaderWevViewSelectActionModeCallback(
            context = context,
            original = callback,
            delegate = { selectionActionModeDelegate },
            onDestroyed = { activeSelectionActionMode = null }
        )
    }

    override fun showContextMenu(): Boolean {
        return super.showContextMenu()
    }
}