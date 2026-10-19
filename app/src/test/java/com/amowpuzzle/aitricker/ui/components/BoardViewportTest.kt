package com.amowpuzzle.aitricker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardViewportTest {
    @Test
    fun `inverse tap transform follows zoom and pan`() {
        val viewport = BoardViewport(scale = 4f, panX = 60f, panY = -120f)
        val x = (115f - 200f) * viewport.scale + 200f + viewport.panX
        val y = (410f - 300f) * viewport.scale + 300f + viewport.panY
        assertEquals(115f, viewport.boardX(x, 400f), 0.0001f)
        assertEquals(410f, viewport.boardY(y, 600f), 0.0001f)
    }

    @Test
    fun `pinch keeps touched board point stationary`() {
        val start = BoardViewport(scale = 2f)
        val result = start.transform(1.5f, 0f, 0f, 260f, 250f, 400f, 600f, 400f, 600f)
        assertEquals(start.boardX(260f, 400f), result.boardX(260f, 400f), 0.0001f)
        assertEquals(start.boardY(250f, 600f), result.boardY(250f, 600f), 0.0001f)
    }

    @Test
    fun `fit resets pan and zoom caps at eight`() {
        val max = BoardViewport().transform(30f, 10000f, -10000f, 200f, 300f, 400f, 600f, 400f, 400f)
        assertEquals(8f, max.scale, 0f)
        assertEquals(1400f, max.panX, 0f)
        assertEquals(-1300f, max.panY, 0f)
        assertEquals(BoardViewport(), max.transform(0.01f, 0f, 0f, 200f, 300f, 400f, 600f, 400f, 400f))
    }

    @Test
    fun `master zoom reaches sixteen and both board edges stay reachable with accurate taps`() {
        val start = BoardViewport(scale = 8f)
        val near = start.transform(4f, 10000f, 10000f, 200f, 300f, 400f, 600f, 400f, 400f, maxScale = 16f)
        val far = start.transform(4f, -10000f, -10000f, 200f, 300f, 400f, 600f, 400f, 400f, maxScale = 16f)
        assertEquals(16f, near.scale, 0f)
        assertEquals(16f, far.scale, 0f)
        assertEquals(0f, near.boardX(0f, 400f), 0.0001f)
        assertEquals(100f, near.boardY(0f, 600f), 0.0001f)
        assertEquals(400f, far.boardX(400f, 400f), 0.0001f)
        assertEquals(500f, far.boardY(600f, 600f), 0.0001f)
        val result = start.transform(1.5f, 0f, 0f, 250f, 320f, 400f, 600f, 400f, 400f, maxScale = 16f)
        assertEquals(start.boardX(250f, 400f), result.boardX(250f, 400f), 0.0001f)
        assertEquals(start.boardY(320f, 600f), result.boardY(320f, 600f), 0.0001f)
        assertEquals(BoardViewport(), far.transform(0.01f, 0f, 0f, 200f, 300f, 400f, 600f, 400f, 400f, maxScale = 16f))
    }

    @Test
    fun `grandmaster zoom reaches twenty four with reachable edges and reversible touch mapping`() {
        val start = BoardViewport(scale = 12f, panX = 30f, panY = -60f)
        val near = start.transform(4f, 100000f, 100000f, 180f, 320f, 360f, 640f, 320f, 500f, maxScale = 24f)
        val far = start.transform(4f, -100000f, -100000f, 180f, 320f, 360f, 640f, 320f, 500f, maxScale = 24f)
        assertEquals(24f, near.scale, 0f)
        assertEquals(24f, far.scale, 0f)
        assertEquals(20f, near.boardX(0f, 360f), 0.0001f)
        assertEquals(70f, near.boardY(0f, 640f), 0.0001f)
        assertEquals(340f, far.boardX(360f, 360f), 0.0001f)
        assertEquals(570f, far.boardY(640f, 640f), 0.0001f)
        val anchored = start.transform(1.5f, 0f, 0f, 240f, 390f, 360f, 640f, 320f, 500f, maxScale = 24f)
        assertEquals(start.boardX(240f, 360f), anchored.boardX(240f, 360f), 0.0001f)
        assertEquals(start.boardY(390f, 640f), anchored.boardY(390f, 640f), 0.0001f)
        assertEquals(BoardViewport(), far.transform(0.001f, 0f, 0f, 180f, 320f, 360f, 640f, 320f, 500f, maxScale = 24f))
    }

    @Test
    fun `dense taps reject near ties and empty space`() {
        assertNull(unambiguousTargetIndex(listOf(2f, 2.05f), 3f, 0.1f))
        assertNull(unambiguousTargetIndex(listOf(4f, 6f), 3f, 0.1f))
        assertNull(unambiguousTargetIndex(emptyList(), 3f, 0.1f))
        assertEquals(1, unambiguousTargetIndex(listOf(2f, 0.2f, 3f), 3f, 0.1f))
    }
}
