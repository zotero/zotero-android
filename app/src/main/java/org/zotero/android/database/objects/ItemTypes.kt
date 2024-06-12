package org.zotero.android.database.objects

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

        fun iconName(rawType: String, contentType: String?): String {
            when (rawType) {
                "artwork" -> {
                    return "item_type_artwork"
                }
                "attachment" -> {
                    if (contentType?.contains("pdf") == true) {
                        return "item_type_pdf"
                    }
                    return "item_type_document"
                }
                "audioRecording" -> {
                    return "item_type_audiorecording"
                }
                "book" -> {
                    return "item_type_book"
                }
                "bookSection" -> {
                    return "item_type_booksection"
                }
                "bill" -> {
                    return "item_type_bill"
                }
                "blogPost" -> {
                    return "item_type_blogpost"
                }
                "case" -> {
                    return "item_type_case"
                }
                "computerProgram" -> {
                    return "item_type_computerprogram"
                }
                "conferencePaper" -> {
                    return "item_type_conferencepaper"
                }
                "dictionaryEntry" -> {
                    return "item_type_dictionaryentry"
                }
                "document" -> {
                    return "item_type_document"
                }
                "email" -> {
                    return "item_type_email"
                }
                "encyclopediaArticle" -> {
                    return "item_type_encyclopediaarticle"
                }
                "film" -> {
                    return "item_type_film"
                }
                "forumPost" -> {
                    return "item_type_forumpost"
                }
                "hearing" -> {
                    return "item_type_hearing"
                }
                "instantMessage" -> {
                    return "item_type_instantmessage"
                }
                "interview" -> {
                    return "item_type_interview"
                }
                "journalArticle" -> {
                    return "item_type_journalarticle"
                }
                "letter" -> {
                    return "item_type_letter"
                }
                "magazineArticle" -> {
                    return "item_type_magazinearticle"
                }
                "map" -> {
                    return "item_type_map"
                }
                "manuscript" -> {
                    return "item_type_manuscript"
                }
                "note" -> {
                    return "item_type_note"
                }
                "newspaperArticle" -> {
                    return "item_type_newspaperarticle"
                }
                "patent" -> {
                    return "item_type_patent"
                }
                "podcast" -> {
                    return "item_type_podcast"
                }
                "presentation" -> {
                    return "item_type_presentation"
                }
                "radioBroadcast" -> {
                    return "item_type_radiobroadcast"
                }
                "report" -> {
                    return "item_type_report"
                }
                "statute" -> {
                    return "item_type_statute"
                }
                "thesis" -> {
                    return "item_type_thesis"
                }
                "tvBroadcast" -> {
                    return "item_type_tvbroadcast"
                }
                "videoRecording" -> {
                    return "item_type_videorecording"
                }
                "webpage" -> {
                    return "item_type_webpage"
                }
                else -> {
                    return "item_type_document"
                }
            }
        }

    }

}