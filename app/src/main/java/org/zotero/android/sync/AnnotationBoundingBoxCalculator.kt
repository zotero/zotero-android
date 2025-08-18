package org.zotero.android.sync

import android.graphics.PointF
import android.graphics.RectF

class AnnotationBoundingBoxCalculator {
    companion object {
        fun boundingBox(paths: List<List<PointF>>, lineWidth: Float): RectF {
            if (paths.isEmpty()) {
                return RectF()
            }

            var minX: Float = Float.MAX_VALUE
            var minY: Float = Float.MAX_VALUE
            var maxX = 0.0F
            var maxY = 0.0F

            for (path in paths) {
                for (point in path) {
                    val _minX = point.x - lineWidth
                    val _maxX = point.x + lineWidth
                    val _minY = point.y - lineWidth
                    val _maxY = point.y + lineWidth

                    if (_minX < minX) {
                        minX = _minX
                    }
                    if (_maxX > maxX) {
                        maxX = _maxX
                    }
                    if (_minY < minY) {
                        minY = _minY
                    }
                    if (_maxY > maxY) {
                        maxY = _maxY
                    }
                }
            }

            return RectF(
                /* left = */ minX,
                /* top = */ maxY,
                /* right = */ maxX,
                /* bottom = */ minY
            )
        }

        fun boundingBox(rects: List<RectF>): RectF {
            if (rects.isEmpty()) {
                return RectF()
            }

            var minX: Float = Float.MAX_VALUE
            var minY: Float = Float.MAX_VALUE
            var maxX = 0.0f
            var maxY = 0.0f

            for (rect in rects) {
                if (rect.left < minX) {
                    minX = rect.left
                }
                if (rect.bottom < minY) {
                    minY = rect.bottom
                }
                if (rect.right > maxX) {
                    maxX = rect.right
                }
                if (rect.top > maxY) {
                    maxY = rect.top
                }
            }

            return RectF(
                /* left = */ minX,
                /* top = */ maxY,
                /* right = */ maxX,
                /* bottom = */ minY
            )
        }
    }


}