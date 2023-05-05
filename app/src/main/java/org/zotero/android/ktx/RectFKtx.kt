package org.zotero.android.ktx

import android.graphics.RectF


fun RectF.rounded(places: Int): RectF {
    return RectF(
        this.left.rounded(places),
        this.top.rounded(places),
        this.right.rounded(places),
        this.bottom.rounded(places),
    )
}