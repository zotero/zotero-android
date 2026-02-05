package org.zotero.android.screens.htmlepub.reader.web

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.View
import android.webkit.WebView

class HtmlEpubReaderWebView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

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
        return startActionModeForChild(this, HtmlEpubReaderWevViewSelectActionModeCallback())
    }

    public override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode {
        return startActionModeForChild(this, HtmlEpubReaderWevViewSelectActionModeCallback(), type)
    }

    override fun showContextMenu(): Boolean {
        return super.showContextMenu()
    }
}