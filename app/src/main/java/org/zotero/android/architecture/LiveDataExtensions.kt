package org.zotero.android.architecture

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.subscribe(
    owner: LifecycleOwner,
    func: (T) -> Unit
) {
    val observer = Observer<T> { func(it) }
    observe(owner, observer)
}
