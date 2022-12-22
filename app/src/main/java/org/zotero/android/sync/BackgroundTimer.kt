package org.zotero.android.sync

import java.util.Timer
import kotlin.concurrent.timerTask

class BackgroundTimer(
    private val timeIntervalMs: Long,
    private val eventHandler: (() -> Unit)?
) {
    private enum class State {
        suspended,
        resumed
    }

    private var state: State = State.suspended

    private lateinit var timer: Timer

    private fun initTimer() {
        timer = Timer()
        timer.schedule(timerTask {
            eventHandler?.let { it() }
            this@BackgroundTimer.suspend()
        }, timeIntervalMs)
    }

    fun deinit() {
        timer.cancel()
    }

    fun resume() {
        if (this.state == State.resumed) {
            return
        }
        this.state = State.resumed
        initTimer()
    }

    fun suspend() {
        if (this.state == State.suspended) {
            return
        }
        this.state = State.suspended
        deinit()
    }
}