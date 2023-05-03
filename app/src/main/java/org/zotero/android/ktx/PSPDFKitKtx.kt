package org.zotero.android.ktx

import com.pspdfkit.annotations.Annotation
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