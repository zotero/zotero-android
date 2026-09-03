package org.zotero.android.screens.reader.web

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import kotlin.math.abs

class ReaderCustomWebView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

    var selectionActionModeDelegate: ReaderSelectionActionModeDelegate? = null

    private var activeSelectionActionMode: ActionMode? = null

    var onUserGestureDetected: (() -> Unit)? = null
    private var gestureDownX = 0f
    private var gestureDownY = 0f
    private var gestureAlreadyReportedThisTouch = false
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureDownX = ev.x
                gestureDownY = ev.y
                gestureAlreadyReportedThisTouch = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!gestureAlreadyReportedThisTouch) {
                    gestureAlreadyReportedThisTouch = true
                    onUserGestureDetected?.invoke()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!gestureAlreadyReportedThisTouch &&
                    (abs(ev.x - gestureDownX) > touchSlop || abs(ev.y - gestureDownY) > touchSlop)
                ) {
                    gestureAlreadyReportedThisTouch = true
                    onUserGestureDetected?.invoke()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

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