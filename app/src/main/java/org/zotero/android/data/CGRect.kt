package org.zotero.android.data

data class CGRect(
    var x:Double,
    var y:Double,
    var width:Double,
    var height:Double,
) {
    val minX = x
    val minY = y
    val maxX = x + width
    val maxY = y + height
}
