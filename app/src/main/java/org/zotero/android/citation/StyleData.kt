package org.zotero.android.citation

import org.zotero.android.database.objects.RStyle

data class StyleData(
    val filename: String,
    val defaultLocaleId: String?,
    val supportsBibliography: Boolean,
) {
    companion object {
        fun fromRStyle(style: RStyle): StyleData {
            return StyleData(
                filename = style.dependency?.filename ?: style.filename,
                defaultLocaleId = style.defaultLocale.ifEmpty { null },
                supportsBibliography = style.supportsBibliography
            )
        }
    }
}
