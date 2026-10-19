package com.amowpuzzle.aitricker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardReadabilityTest {
    @Test fun `dense boards have the same readable ink size as the reference star`() {
        for (density in listOf(1f, 2f, 3f)) {
            val width = 360f * density
            val height = 480f * density
            for (cols in listOf(61, 98, 118, 180, 240)) {
                val cell = width / cols
                val scale = readableBoardScale(cell, width, height, density, 32f)
                assertEquals(7f, cell * scale / density, 0.0001f)
                assertTrue(cell * scale * 0.28f / density >= 1.95f)
            }
        }
    }

    @Test fun `intro boards retain their full shape and zoom stays bounded`() {
        assertEquals(1f, readableBoardScale(50f, 360f, 480f, 1f, 8f), 0f)
        assertEquals(32f, readableBoardScale(0.01f, 360f, 480f, 1f, 32f), 0f)
    }

    @Test fun `read view can pan to both edges and return to fit without shifting tap targets`() {
        val scale = readableBoardScale(360f / 118f, 360f, 480f, 1f, 32f)
        val initial = BoardViewport(scale = scale)
        val left = initial.transform(1f, 10000f, 0f, 180f, 240f, 360f, 480f, 360f, 360f, 32f)
        val right = initial.transform(1f, -10000f, 0f, 180f, 240f, 360f, 480f, 360f, 360f, 32f)
        assertEquals(0f, left.boardX(0f, 360f), 0.0001f)
        assertEquals(360f, right.boardX(360f, 360f), 0.0001f)
        assertEquals(BoardViewport(), right.transform(1f / scale, 0f, 0f, 180f, 240f, 360f, 480f, 360f, 360f, 32f))
    }
}
