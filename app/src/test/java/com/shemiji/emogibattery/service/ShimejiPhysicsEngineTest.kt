package com.shemiji.emogibattery.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShimejiPhysicsEngineTest {
    @Test fun traversalVisitsEveryRequestedState() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 3f)
        engine.restore(300f, 900f, ShimejiMotion.WALKING_LEFT, ScreenEdge.BOTTOM)
        val visited = mutableSetOf<ShimejiMotion>()
        repeat(3_000) { visited += engine.motion; engine.tick(50) }
        listOf(
            ShimejiMotion.WALKING_LEFT, ShimejiMotion.CLIMBING_LEFT,
            ShimejiMotion.JUMPING_LEFT_TO_RIGHT, ShimejiMotion.CLIMBING_RIGHT,
            ShimejiMotion.JUMPING_RIGHT_TO_LEFT, ShimejiMotion.TOP_WALKING_RIGHT,
            ShimejiMotion.FALLING, ShimejiMotion.BOUNCING,
        ).forEach { assertTrue("Missing state: $it", it in visited) }
    }

    @Test fun releaseInMiddleFallsAndBouncesOnFloor() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 1f)
        engine.beginDrag(); engine.dragTo(250f, 300f); engine.endDrag()
        assertEquals(ShimejiMotion.FALLING, engine.motion)
        repeat(200) { if (engine.motion != ShimejiMotion.BOUNCING) engine.tick(50) }
        assertEquals(ShimejiMotion.BOUNCING, engine.motion)
        repeat(11) { engine.tick(50) }
        assertEquals(ScreenEdge.BOTTOM, engine.edge)
        assertEquals(900f, engine.y, 0.01f)
        assertTrue(engine.motion == ShimejiMotion.WALKING_LEFT || engine.motion == ShimejiMotion.WALKING_RIGHT)
    }
}
