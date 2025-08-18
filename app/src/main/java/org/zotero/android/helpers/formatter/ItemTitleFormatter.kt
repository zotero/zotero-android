package org.zotero.android.helpers.formatter

import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemField

class ItemTitleFormatter {
    companion object {
        const val nameCountLimit: Int = 4


        fun displayTitle(item: RItem): String {
            when (item.rawType) {
                ItemTypes.letter ->
                    return letterDisplayTitle(baseTitle = item.baseTitle, creators = item.creators)
                ItemTypes.interview ->
                    return interviewDisplayTitle(
                        baseTitle = item.baseTitle,
                        creators = item.creators
                    )
                ItemTypes.case ->
                    return caseDisplayTitle(
                        baseTitle = item.baseTitle,
                        fields = item.fields,
                        creators = item.creators
                    )
                else ->
                    return item.baseTitle
            }
        }

        private fun letterDisplayTitle(baseTitle: String, creators: List<RCreator>): String {
            if (!baseTitle.isEmpty()) {
                return baseTitle
            }

            val names = separatedCreators(
                results = creators.filter { it.rawType == "recipient" },
                limit = nameCountLimit
            )
            if (names.isEmpty()) {
                return "[Letter]"
            }
            return "[Letter to $names]"
        }

        private fun interviewDisplayTitle(baseTitle: String, creators: List<RCreator>): String {
            if (!baseTitle.isEmpty()) {
                return baseTitle
            }
            val names = separatedCreators(
                results = creators.filter { it.rawType == "interviewer" },
                limit = nameCountLimit
            )
            if (names.isEmpty()) {
                return "[Interview]"
            }
            return "[Interview by $names]"
        }

        private fun separatedCreators(results: List<RCreator>, limit: Int): String {
            val names = creatorNames(results, limit)
            return when (names.size) {
                0 ->
                    ""

                1 ->
                    names[0]

                2 ->
                    names[0] + " and " + names[1]

                3 ->
                    names[0] + ", " + names[1] + " and " + names[2]

                else ->
                    names[0] + " et al."
            }
        }

        private fun creatorNames(results: List<RCreator>, limit: Int): List<String> {
            if (results.isEmpty()) {
                return listOf()
            }
            val sortedResults = results.sortedBy { it.orderId }

            var index = 0
            val names = mutableListOf<String>()

            while (index < sortedResults.size && names.size < limit) {
                val name = sortedResults[index].summaryName
                index += 1

                if (!name.isEmpty()) {
                    names.add(name)
                }
            }
            return names
        }

        private fun caseDisplayTitle(baseTitle: String, fields: List<RItemField>, creators: List<RCreator>): String {
            if (!baseTitle.isEmpty()) {
                var title = baseTitle
                val field = fields.firstOrNull { it.key == FieldKeys.Item.reporter }
                if (field != null && !field.value.isEmpty()) {
                    title += " (${field.value})"
                } else {
                    val field = fields.firstOrNull { it.key == FieldKeys.Item.court }
                    if (field != null && !field.value.isEmpty()) {
                        title += " (${field.value})"
                    }
                }
                return title
            }

            val parts = mutableListOf<String>()
            val fieldCourt = fields.firstOrNull { it.key == FieldKeys.Item.court }
            if (fieldCourt != null && !fieldCourt.value.isEmpty()) {
                parts.add(fieldCourt.value)
            }

            val field =
                fields.firstOrNull { it.key == FieldKeys.Item.date && it.baseKey == FieldKeys.Item.date }
            if (field != null && !field.value.isEmpty()) {
                parts.add(field.value)
            }

            val creator =
                creators.filter { it.primary }.minByOrNull { it.orderId }
            if (creator != null) {
                val name = creator.summaryName
                if (!name.isEmpty()) {
                    parts.add(name)
                }
            }

            return "[" + parts.joinToString(separator =  ", ") + "]"
        }
    }

}
