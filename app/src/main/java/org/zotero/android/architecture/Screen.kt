package org.zotero.android.architecture

import androidx.lifecycle.LifecycleOwner

/** Represents a screen that can observe ViewChanges. */
interface Screen<STATE : ViewState, EFFECT : ViewEffect> : ViewEffectConsumer<EFFECT>, LifecycleOwner {

    /** Called when there is a new [ViewState] to render. */
    fun render(state: STATE)
}

interface ViewEffectConsumer<EFFECT : ViewEffect> : LifecycleOwner {
    fun consumeEffect(consumable: Consumable<EFFECT>) =
        when (val effect = consumable.consume()) {
            null -> Unit
            else -> trigger(effect)
        }

    /** Called when there is a new [ViewEffect] to trigger. */
    fun trigger(effect: EFFECT)

    /**
     * Used to subscribe to LiveData. For Activities, we can use the default
     * implementation, but for Fragments we should override this value with
     * ViewLifecycleOwner so that events are not emitted when the view is no
     * longer available.
     */
    val lifecycleOwner: LifecycleOwner get() = this
}
