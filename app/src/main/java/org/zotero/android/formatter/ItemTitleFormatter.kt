package org.zotero.android.formatter

import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.RCreator
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RItemField

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
            when (names.size) {
                0 ->
                    return ""
                1 ->
                    return names[0]
                2 ->
                    return names[0] + " and " + names[1]
                3 ->
                    return names[0] + ", " + names[1] + " and " + names[2]
                else ->
                    return names[0] + " et al."
            }
        }

        private fun creatorNames(results: List<RCreator>, limit: Int): List<String> {
            if (results.isEmpty()) {
                return listOf()
            }
            val sortedResults = results.sortedBy { "orderId" }

            var index = 0
            var names = mutableListOf<String>()

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
                val field = fields.filter{it.key == FieldKeys.Item.reporter}.firstOrNull()
                if (field != null && !field.value.isEmpty()) {
                    title += " (${field.value})"
                } else {
                    val field = fields.filter{it.key == FieldKeys.Item.court}.firstOrNull()
                    if (field != null && !field.value.isEmpty()) {
                        title += " (${field.value})"
                    }
                }
                return title
            }

            var parts = mutableListOf<String>()
            val fieldCourt = fields.filter { it.key == FieldKeys.Item.court }.firstOrNull()
            if (fieldCourt != null && !fieldCourt.value.isEmpty()) {
                parts.add(fieldCourt.value)
            }

            val field = fields.filter { it.key ==  FieldKeys.Item.date && it.baseKey == FieldKeys.Item.date}.firstOrNull()
            if (field != null && !field.value.isEmpty()) {
                parts.add(field.value)
            }

            val creator =
                creators.filter { it.primary == true }.sortedBy { "orderId" }.firstOrNull()
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
