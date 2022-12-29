package org.zotero.android.architecture.database.objects

import org.zotero.android.uicomponents.Drawables

class ItemTypes {
    companion object {
        const val note = "note"
        const val attachment = "attachment"
        const val case = "case"
        const val letter = "letter"
        const val interview = "interview"
        const val webpage = "webpage"
        const val annotation = "annotation"
        const val document = "document"

        var excludedFromTypePicker: Set<String> = setOf(attachment, annotation)

        fun iconName(rawType: String, contentType: String?): Int {
            //TODO implement
            return Drawables.attachment_list_pdf
        }

    }

}