package org.zotero.android.ktx

import com.pspdfkit.annotations.Annotation
import com.pspdfkit.document.PdfDocument

fun PdfDocument.annotation(page: Int, key: String): Annotation? {
    return annotationProvider.getAnnotations(page).firstOrNull { it.key == key || it.uuid == key }
}