package org.zotero.android.dashboard.data

import java.util.UUID

data class ItemDetailCreator(
    var id: UUID,
    var type: String,
    var primary: Boolean,
    var localizedType:String,
    var fullName: String,
    var firstName: String,
    var lastName: String,
    var namePresentation: NamePresentation
) {
    enum class NamePresentation {
        separate,
        full;
    }

    fun toggle() {
        this.namePresentation =
            if (namePresentation == NamePresentation.full) NamePresentation.separate else NamePresentation.full
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

    fun change() {
//        if (namePresentation == this.namePresentation) {
//            return
//        }

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



    companion object {
        fun init(
            firstName: String,
            lastName: String,
            fullName: String,
            type: String,
            primary: Boolean,
            localizedType: String
        ): ItemDetailCreator {
            return ItemDetailCreator(
                id = UUID.randomUUID(),
                type = type,
                primary = primary,
                localizedType = localizedType,
                fullName = fullName,
                firstName = firstName,
                lastName = lastName,
                namePresentation =
                if (fullName.isEmpty()) NamePresentation.separate else NamePresentation.full
            )
        }

        fun init(type: String, primary: Boolean, localizedType: String, namePresentation: NamePresentation): ItemDetailCreator {
            return ItemDetailCreator(
                id = UUID.randomUUID(),
                type = type,
                primary = primary,
                localizedType = localizedType,
                fullName = "",
                firstName = "",
                lastName = "",
                namePresentation = namePresentation
            )

        }
    }

}