package com.amowpuzzle.aitricker.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/** A pan or any two-finger gesture must never become an arrow tap on finger release. */
internal suspend fun PointerInputScope.detectBoardGestures(
    onTransform: (anchor: Offset, pan: Offset, zoom: Float) -> Unit,
    onTap: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        var usedMultiplePointers = false
        var transformed = false
        var accumulatedPan = Offset.Zero
        var accumulatedZoom = 1f
        var tapPosition = down.position
        do {
            val event = awaitPointerEvent()
            val cancelled = event.changes.any { it.isConsumed }
            if (event.changes.count { it.pressed || it.previousPressed } > 1) {
                usedMultiplePointers = true
            }
            event.changes.firstOrNull { it.id == down.id }?.let { tapPosition = it.position }
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (!transformed) {
                accumulatedPan += pan
                accumulatedZoom *= zoom
                val zoomMotion = abs(1f - accumulatedZoom) * event.calculateCentroidSize(useCurrent = false)
                transformed = accumulatedPan.getDistance() > viewConfiguration.touchSlop ||
                    zoomMotion > viewConfiguration.touchSlop
            }
            if (transformed && !cancelled) {
                if (zoom != 1f || pan != Offset.Zero) {
                    onTransform(event.calculateCentroid(useCurrent = false), pan, zoom)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
            val allReleased = event.changes.none { it.pressed }
            if (allReleased && !cancelled && !transformed && !usedMultiplePointers) {
                event.changes.forEach { it.consume() }
                onTap(tapPosition)
            }
        } while (!cancelled && !allReleased)
    }
}