package org.zotero.android.ktx

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.sqrt

fun RectF.rounded(places: Int): RectF {
    return RectF(
        this.left.rounded(places),
        this.top.rounded(places),
        this.right.rounded(places),
        this.bottom.rounded(places),
    )
}

fun RectF.distance(rect: RectF): Float {
    val left = rect.right < this.left
    val right = this.right < rect.left
    val bottom = rect.top < this.bottom
    val top = this.top < rect.bottom

    if (top && left) {
        return distance((this.left to this.top), (rect.right to rect.bottom))
    } else if (left && bottom) {
        return distance((this.left to this.bottom), (rect.right to rect.top))
    } else if (bottom && right) {
        return distance((this.right to this.bottom), (rect.left to rect.top))
    } else if (right && top) {
        return distance((this.right to this.top), (rect.left to rect.bottom))
    } else if (left) {
        return this.left - rect.right
    } else if (right) {
        return rect.left - this.right
    } else if (bottom) {
        return this.bottom - rect.top
    } else if (top) {
        return rect.bottom - this.top
    }

    return 0F
}

private fun distance(fromPoint: Pair<Float, Float>, toPoint: Pair<Float, Float>): Float {
    return sqrt(((fromPoint.first - toPoint.first) * (fromPoint.first - toPoint.first))
            + (fromPoint.second - toPoint.second) * (fromPoint.second - toPoint.second))
}

fun PointF.rounded(places: Int): PointF {
    return PointF(
        this.x.rounded(places),
        this.y.rounded(places)
    )
}

