package org.zotero.android.dashboard.data

import java.util.UUID

class ItemDetailCreator {
    enum class NamePresentation {
        separate,
        full;

        fun toggle(): NamePresentation {
            return if (this == full) separate else full
        }
    }

    lateinit var id: UUID
    var type: String? = null
    var primary: Boolean? = null
    lateinit var localizedType: String
    var fullName: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var namePresentation: NamePresentation ? = null
        set(newValue) {
        field = newValue
            change(namePresentation =  newValue)
    }

    val name: String get() {
        when (this.namePresentation) {
            NamePresentation.full ->
            return this.fullName!!
            NamePresentation.separate -> {
                if (this.lastName.isNullOrEmpty()) {
                    return this.firstName!!
                }
                if (this.firstName.isNullOrEmpty()) {
                    return this.lastName!!
                }
                return this.lastName!! + ", " + this.firstName!!
            }
            null -> throw java.lang.IllegalArgumentException("namePresentation should not be null")
        }
    }

    val isEmpty: Boolean get() {
        when (this.namePresentation) {
            NamePresentation.full ->
            return this.fullName.isNullOrEmpty()
            NamePresentation.separate ->
            return this.firstName.isNullOrEmpty() && this.lastName.isNullOrEmpty()
            null -> return true
        }
    }

    private fun change(namePresentation: NamePresentation?) {
        if (namePresentation == this.namePresentation) {
            return
        }

        when (namePresentation) {
            NamePresentation.full -> {
                this.fullName = this.firstName + (if (this.firstName.isNullOrEmpty()) "" else " ") + this.lastName
                this.firstName = ""
                this.lastName = ""
            }
            NamePresentation.separate -> {
                if (this.fullName.isNullOrEmpty()) {
                    this.firstName = ""
                    this.lastName = ""
                    return
                }

                if (!(this.fullName?.contains(" ") ?: false)) {
                    this.lastName = this.fullName
                    this.firstName = ""
                    return
                }

                val fullName = this.fullName
                if (fullName != null) {
                    val components = fullName.split(" ")
                    this.firstName = components.dropLast(1).joinToString(separator = " ")
                    this.lastName = components.lastOrNull() ?: ""
                }

            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemDetailCreator

        if (type != other.type) return false
        if (primary != other.primary) return false
        if (fullName != other.fullName) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (primary?.hashCode() ?: 0)
        result = 31 * result + (fullName?.hashCode() ?: 0)
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun init(firstName: String, lastName: String, fullName: String, type: String, primary: Boolean, localizedType: String): ItemDetailCreator {
            return ItemDetailCreator().apply {
                this.id = UUID.randomUUID()
                this.type = type
                this.primary = primary
                this.localizedType = localizedType
                this.fullName = fullName
                this.firstName = firstName
                this.lastName = lastName
                this.namePresentation = if(fullName.isEmpty()) NamePresentation.separate else NamePresentation.full
            }

        }

        fun init(type: String, primary: Boolean, localizedType: String, namePresentation: NamePresentation): ItemDetailCreator {
            return ItemDetailCreator().apply {
                this.id = UUID.randomUUID()
                this.type = type
                this.primary = primary
                this.localizedType = localizedType
                this.fullName = ""
                this.firstName = ""
                this.lastName = ""
                this.namePresentation = namePresentation
            }

        }
    }

}