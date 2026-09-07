package org.zotero.android.screens.reader.web

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.get
import androidx.core.view.size
import org.zotero.android.uicomponents.Strings

private const val MENU_ITEM_ID_HIGHLIGHT = Menu.FIRST + 1000
private const val MENU_ITEM_ID_UNDERLINE = Menu.FIRST + 1001

class ReaderWevViewSelectActionModeCallback(
    private val context: Context,
    private val original: ActionMode.Callback,
    private val delegate: () -> ReaderSelectionActionModeDelegate?,
    private val onDestroyed: () -> Unit,
) : ActionMode.Callback2() {

    private var originalMenuPrepared = false

    private val frameworkCopyLabel: CharSequence? by lazy {
        val resId = Resources.getSystem().getIdentifier("copy", "string", "android")
        if (resId != 0) runCatching { context.getText(resId) }.getOrNull() else null
    }

    private fun isCopyItem(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.copy) {
            return true
        }
        val label = frameworkCopyLabel ?: return false
        return item.title?.toString() == label.toString()
    }
  
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        return original.onCreateActionMode(mode, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        if (!originalMenuPrepared) {
            original.onPrepareActionMode(mode, menu)
            originalMenuPrepared = true
        }

        val menuItems: MutableList<MenuItem> = ArrayList()
        for (i in 0..<menu.size) {
            menuItems.add(menu[i])
        }

        menu.clear()

        var orderIndex = 0

        menu.add(
            Menu.NONE,
            MENU_ITEM_ID_HIGHLIGHT,
            Menu.NONE,
            context.getString(Strings.pdf_highlight)
        )
        menu.add(
            Menu.NONE,
            MENU_ITEM_ID_UNDERLINE,
            Menu.NONE,
            context.getString(Strings.pdf_underline)
        )

        for (item in menuItems) {
            if (item.itemId != MENU_ITEM_ID_HIGHLIGHT && item.itemId != MENU_ITEM_ID_UNDERLINE) {
                menu.add(item.groupId, item.itemId, orderIndex++, item.title).intent = item.intent
            }
        }

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ITEM_ID_HIGHLIGHT -> {
                delegate()?.onHighlight()
                mode.finish()
                true
            }

            MENU_ITEM_ID_UNDERLINE -> {
                delegate()?.onUnderline()
                mode.finish()
                true
            }

            else -> {
                if (isCopyItem(item) && delegate()?.onCopy() == true) {
                    mode.finish()
                    true
                } else {
                    original.onActionItemClicked(mode, item)
                }
            }
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        onDestroyed()
        original.onDestroyActionMode(mode)
    }

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        val originalCallback2 = original as? ActionMode.Callback2
        if (originalCallback2 != null) {
            originalCallback2.onGetContentRect(mode, view, outRect)
        } else {
            super.onGetContentRect(mode, view, outRect)
        }
    }
}
