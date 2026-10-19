package com.amowpuzzle.aitricker.ui.components

import kotlin.math.max
import kotlin.math.min

/** Level 30's 61-cell canvas is the reference for a consistent reading size. */
internal fun readableBoardScale(
    cellSize: Float,
    widthPx: Float,
    heightPx: Float,
    density: Float,
    maxScale: Float
): Float {
    require(cellSize > 0f && widthPx > 0f && heightPx > 0f && density > 0f && maxScale >= 1f)
    // With 28% ink width this yields a stroke of at least 1.96 dp in Read view.
    val referenceCell = max(min(widthPx, heightPx) / 61f, 7f * density)
    return (referenceCell / cellSize).coerceIn(1f, maxScale)
}
