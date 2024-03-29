package org.zotero.android.ktx

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.rounded(places: Int): Double {
    val decimal = BigDecimal(this).setScale(places, RoundingMode.HALF_EVEN)
    return decimal.toDouble()
}

fun Float.rounded(places: Int): Float {
    val decimal = BigDecimal(this.toDouble()).setScale(places, RoundingMode.HALF_EVEN)
    return decimal.toFloat()
}