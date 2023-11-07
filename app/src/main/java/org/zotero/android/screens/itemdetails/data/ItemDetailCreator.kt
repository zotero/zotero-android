package org.zotero.android.screens.itemdetails.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ItemDetailCreator(
    var id: String,
    var type: String,
    var primary: Boolean,
    var localizedType:String,
    var fullName: String,
    var firstName: String,
    var lastName: String,
    var namePresentation: NamePresentation
): Parcelable {
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
            return this.fullName
            NamePresentation.separate -> {
                if (this.lastName.isEmpty()) {
                    return this.firstName
                }
                if (this.firstName.isEmpty()) {
                    return this.lastName
                }
                return this.lastName + ", " + this.firstName
            }
        }
    }

    val isEmpty: Boolean get() {
        return when (this.namePresentation) {
            NamePresentation.full ->
                this.fullName.isEmpty()
            NamePresentation.separate ->
                this.firstName.isEmpty() && this.lastName.isEmpty()
        }
    }

    fun change() {
//        if (namePresentation == this.namePresentation) {
//            return
//        }

        when (namePresentation) {
            NamePresentation.full -> {
                this.fullName = this.firstName + (if (this.firstName.isEmpty()) "" else " ") + this.lastName
                this.firstName = ""
                this.lastName = ""
            }
            NamePresentation.separate -> {
                if (this.fullName.isEmpty()) {
                    this.firstName = ""
                    this.lastName = ""
                    return
                }

                if (!(this.fullName.contains(" "))) {
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
            uuid: String,
            firstName: String,
            lastName: String,
            fullName: String,
            type: String,
            primary: Boolean,
            localizedType: String
        ): ItemDetailCreator {
            return ItemDetailCreator(
                id = uuid,
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
                id = UUID.randomUUID().toString(),
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