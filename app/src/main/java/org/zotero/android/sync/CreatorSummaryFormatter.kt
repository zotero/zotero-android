package org.zotero.android.sync

import org.zotero.android.database.objects.CreatorTypes
import org.zotero.android.database.objects.RCreator
import org.zotero.android.ZoteroApplication
import org.zotero.android.uicomponents.Strings

class CreatorSummaryFormatter {

    companion object {
        fun summary(allCreators: List<RCreator>): String? {
            val primary = allCreators.filter { it.primary }
            if (primary.isNotEmpty()) {
                return summaryStr(primary)
            }
            val editors = allCreators.filter { it.rawType == CreatorTypes.editor }
            if (editors.isNotEmpty()) {
                return summaryStr(editors)
            }
            val contributors = allCreators.filter { it.rawType == CreatorTypes.contributor }
            if (contributors.isNotEmpty()) {
                return summaryStr(contributors)
            }
            return null
        }

        private fun summaryStr(creators: List<RCreator>): String? {
            val context = ZoteroApplication.instance
            when (creators.size) {
                0 ->
                    return null
                1 ->
                    return creators.firstOrNull()?.summaryName
                2 -> {
                    val sorted = creators.sortedBy { it.orderId }
                    return context.getString(
                        Strings.items_creator_summary_and,
                        sorted.first().summaryName,
                        sorted.last().summaryName
                    )
                }
                else -> {
                    val sorted = creators.sortedBy { it.orderId }
                    return context.getString(
                        Strings.items_creator_summary_etal,
                        sorted.first().summaryName
                    )
                }
            }
        }
    }
}
