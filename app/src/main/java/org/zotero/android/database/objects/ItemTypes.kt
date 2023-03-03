package org.zotero.android.database.objects

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
            when (rawType) {
                "artwork" -> {
                    return Drawables.item_type_artwork
                }
                "attachment" -> {
                    if (contentType?.contains("pdf") == true) {
                        return Drawables.item_type_pdf
                    }
                    return Drawables.item_type_document
                }
                "audioRecording" -> {
                    return Drawables.item_type_audiorecording
                }
                "book" -> {
                    return Drawables.item_type_book
                }
                "bookSection" -> {
                    return Drawables.item_type_booksection
                }
                "bill" -> {
                    return Drawables.item_type_bill
                }
                "blogPost" -> {
                    return Drawables.item_type_blogpost
                }
                "case" -> {
                    return Drawables.item_type_case
                }
                "computerProgram" -> {
                    return Drawables.item_type_computerprogram
                }
                "conferencePaper" -> {
                    return Drawables.item_type_conferencepaper
                }
                "dictionaryEntry" -> {
                    return Drawables.item_type_dictionaryentry
                }
                "document" -> {
                    return Drawables.item_type_document
                }
                "email" -> {
                    return Drawables.item_type_email
                }
                "encyclopediaArticle" -> {
                    return Drawables.item_type_encyclopediaarticle
                }
                "film" -> {
                    return Drawables.item_type_film
                }
                "forumPost" -> {
                    return Drawables.item_type_forumpost
                }
                "hearing" -> {
                    return Drawables.item_type_hearing
                }
                "instantMessage" -> {
                    return Drawables.item_type_instantmessage
                }
                "interview" -> {
                    return Drawables.item_type_interview
                }
                "journalArticle" -> {
                    return Drawables.item_type_journalarticle
                }
                "letter" -> {
                    return Drawables.item_type_letter
                }
                "magazineArticle" -> {
                    return Drawables.item_type_magazinearticle
                }
                "map" -> {
                    return Drawables.item_type_map
                }
                "manuscript" -> {
                    return Drawables.item_type_manuscript
                }
                "note" -> {
                    return Drawables.item_type_note
                }
                "newspaperArticle" -> {
                    return Drawables.item_type_newspaperarticle
                }
                "patent" -> {
                    return Drawables.item_type_patent
                }
                "podcast" -> {
                    return Drawables.item_type_podcast
                }
                "presentation" -> {
                    return Drawables.item_type_presentation
                }
                "radioBroadcast" -> {
                    return Drawables.item_type_radiobroadcast
                }
                "report" -> {
                    return Drawables.item_type_report
                }
                "statute" -> {
                    return Drawables.item_type_statute
                }
                "thesis" -> {
                    return Drawables.item_type_thesis
                }
                "tvBroadcast" -> {
                    return Drawables.item_type_tvbroadcast
                }
                "videoRecording" -> {
                    return Drawables.item_type_videorecording
                }
                "webpage" -> {
                    return Drawables.item_type_webpage
                }
                else -> {
                    return Drawables.item_type_document
                }
            }
        }

    }

}