package org.zotero.android.ktx

import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import org.json.JSONObject
import org.zotero.android.database.objects.AnnotationsConfig

var Annotation.key: String?
    get() {
        return this.customData?.opt(AnnotationsConfig.keyKey)?.toString()
    }
    set(newValue) {
        if (this.customData == null) {
            val key = newValue
            if (key != null) {
                this.customData =
                    JSONObject().put(AnnotationsConfig.keyKey, key)
            }
        } else {
            this.customData?.put(AnnotationsConfig.keyKey, newValue)
        }
    }

val Annotation.isZoteroAnnotation: Boolean
    get() {
        return this.key != null || (this.name ?: "").contains("Zotero")
    }

val Annotation.baseColor: String get() {
    return this.color.let { AnnotationsConfig.colorVariationMap[it] } ?: AnnotationsConfig.defaultActiveColor
}

val Annotation.shouldRenderPreview: Boolean
    get() {
        return (this is SquareAnnotation) || (this is InkAnnotation) || (this is FreeTextAnnotation)
    }

val Annotation.previewId: String get() {
    return this.key ?: this.uuid
}