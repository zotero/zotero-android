package org.zotero.android.architecture

/**
 * A Consumable holds a payload that can only be consumed once.
 */
class Consumable<T>(private val payload: T) {
    private var isConsumed = false

    fun consume(): T? = if (isConsumed) {
        null
    } else {
        isConsumed = true
        payload
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Consumable<*>

        if (payload != other.payload) return false
        if (isConsumed != other.isConsumed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = payload?.hashCode() ?: 0
        result = 31 * result + isConsumed.hashCode()
        return result
    }

    override fun toString(): String =
        "Consumable(payload = $payload, isConsumed = $isConsumed)"
}

fun <T> T.consumable(): Consumable<T> = Consumable(this)
