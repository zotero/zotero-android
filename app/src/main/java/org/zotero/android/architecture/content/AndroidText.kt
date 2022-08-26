package org.zotero.android.architecture.content

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

interface AndroidText {
    fun toString(context: Context): String
}

data class StringText(private val text: String) : AndroidText {
    override fun toString(context: Context) = text
}

@Suppress("UseDataClass") // Cannot use data class with vararg in the constructor.
class StringId(
    @StringRes val id: Int,
    vararg val args: Any
) : AndroidText {

    @Suppress("SpreadOperator") // We need spread operator here.
    override fun toString(context: Context): String {
        return if (args.isEmpty()) {
            return context.getString(id)
        } else {
            context.getString(id, *args)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringId

        if (id != other.id) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + args.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "StringId(id=$id, args=${args.contentToString()})"
    }
}

@Suppress("UseDataClass") // Cannot use data class with vararg in the constructor.
class PluralId(
    @PluralsRes private val id: Int,
    private val quantity: Int,
    private vararg val args: Any,
) : AndroidText {

    @Suppress("SpreadOperator") // We need spread operator here.
    override fun toString(context: Context): String {
        return if (args.isEmpty()) {
            return context.resources.getQuantityString(
                id,
                quantity
            )
        } else {
            context.resources.getQuantityString(
                id,
                quantity,
                *args
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluralId

        if (id != other.id) return false
        if (quantity != other.quantity) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + quantity
        result = 31 * result + args.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "PluralId(id=$id, quantity=$quantity, args=${args.contentToString()})"
    }
}
