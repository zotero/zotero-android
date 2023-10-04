package org.zotero.android.sync

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.ktx.rounded

object AnnotationSplitter {
    fun splitRectsIfNeeded(rects: List<RectF>): List<List<RectF>>? {
        if (rects.isEmpty()) {
            return null
        }
        val sortedRects = rects.sortedWith { lRect, rRect ->
            if (lRect.bottom == rRect.bottom) {
                return@sortedWith rRect.left.compareTo(lRect.left)
            }
            return@sortedWith lRect.bottom.compareTo(rRect.bottom)
        }

        var count = 2
        val splitRects = mutableListOf<List<RectF>>()
        var currentRects = mutableListOf<RectF>()

        for (rect in sortedRects) {
            val size =
                rect.left.rounded(3).toString().length +
                        rect.bottom.rounded(3).toString().length +
                        rect.right.rounded(3).toString().length +
                        rect.top.rounded(3)
                            .toString().length + 6

            if (count + size > AnnotationsConfig.positionSizeLimit) {
                if (currentRects.isNotEmpty()) {
                    splitRects.add(currentRects)
                    currentRects = mutableListOf()
                }
                count = 2
            }
            currentRects.add(rect)
            count += size
        }

        if (currentRects.isNotEmpty()) {
            splitRects.add(currentRects)
        }

        if (splitRects.size == 1) {
            return null
        }
        return splitRects
    }

    fun splitPathsIfNeeded(paths: List<List<PointF>>): List<List<List<PointF>>>? {
        if (paths.isEmpty()) {
            return null
        }

        var count = 2
        val splitPaths = mutableListOf<MutableList<MutableList<PointF>>>()
        var currentLines = mutableListOf<MutableList<PointF>>()
        var currentPoints = mutableListOf<PointF>()

        for (subpaths in paths) {
            if (count + 3 > AnnotationsConfig.positionSizeLimit) {
                if (currentPoints.isNotEmpty()) {
                    currentLines.add(currentPoints)
                    currentPoints = mutableListOf()
                }
                if (currentLines.isNotEmpty()) {
                    splitPaths.add(currentLines)
                    currentLines = mutableListOf()
                }
                count = 2
            }
            count += 3
            for (point in subpaths) {
                val size =
                    point.x.rounded(3).toString().length +
                            point.y.rounded(3).toString().length + 2

                if (count + size > AnnotationsConfig.positionSizeLimit) {
                    if (currentPoints.isNotEmpty()) {
                        currentLines.add(currentPoints)
                        currentPoints = mutableListOf()
                    }
                    if (currentLines.isNotEmpty()) {
                        splitPaths.add(currentLines)
                        currentLines = mutableListOf()
                    }
                    count = 5
                }
                count += size
                currentPoints.add(point)
            }

            currentLines.add(currentPoints)
            currentPoints = mutableListOf()
        }

        if (currentPoints.isNotEmpty()) {
            currentLines.add(currentPoints)
        }
        if (currentLines.isNotEmpty()) {
            splitPaths.add(currentLines)
        }
        if (splitPaths.size == 1) {
            return null
        }
        return splitPaths
    }
}