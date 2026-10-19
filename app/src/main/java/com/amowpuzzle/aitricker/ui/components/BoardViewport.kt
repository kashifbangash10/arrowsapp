package com.amowpuzzle.aitricker.ui.components

import kotlin.math.max

/** Uniform, reversible board transform. Geometry and hit tests use the same coordinates. */
internal data class BoardViewport(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    fun boardX(screenX: Float, width: Float): Float =
        (screenX - width / 2f - panX) / scale + width / 2f

    fun boardY(screenY: Float, height: Float): Float =
        (screenY - height / 2f - panY) / scale + height / 2f

    fun transform(
        zoom: Float,
        deltaX: Float,
        deltaY: Float,
        anchorX: Float,
        anchorY: Float,
        width: Float,
        height: Float,
        boardWidth: Float,
        boardHeight: Float,
        maxScale: Float = 8f
    ): BoardViewport {
        val nextScale = (scale * zoom).coerceIn(1f, maxScale)
        if (nextScale == 1f) return BoardViewport()
        val ratio = nextScale / scale
        val nextX = anchorX - width / 2f - (anchorX - width / 2f - panX) * ratio + deltaX
        val nextY = anchorY - height / 2f - (anchorY - height / 2f - panY) * ratio + deltaY
        // Keep the puzzle reachable; at Fit there is no panning into empty space.
        val limitX = max(0f, (boardWidth * nextScale - width) / 2f)
        val limitY = max(0f, (boardHeight * nextScale - height) / 2f)
        return BoardViewport(
            scale = nextScale,
            panX = nextX.coerceIn(-limitX, limitX),
            panY = nextY.coerceIn(-limitY, limitY)
        )
    }
}

/** Refuse almost-equidistant taps instead of spending a life on an arbitrary piece. */
internal fun unambiguousTargetIndex(
    distances: List<Float>,
    tolerance: Float,
    ambiguityMargin: Float
): Int? {
    var index = -1
    var closest = Float.POSITIVE_INFINITY
    var second = Float.POSITIVE_INFINITY
    distances.forEachIndexed { i, distance ->
        if (distance < closest) {
            second = closest
            closest = distance
            index = i
        } else if (distance < second) {
            second = distance
        }
    }
    return index.takeIf {
        it >= 0 && closest <= tolerance && second - closest > ambiguityMargin
    }
}
