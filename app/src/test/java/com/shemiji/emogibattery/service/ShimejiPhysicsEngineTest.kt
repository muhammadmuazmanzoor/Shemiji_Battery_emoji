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

    @Test fun customizationChangesMovementDistanceAndClampsForCharacterSize() {
        val slow = ShimejiPhysicsEngine().apply {
            configure(600, 1_000, 100, 0, 0.5f)
            restore(300f, 900f, ShimejiMotion.WALKING_RIGHT, ScreenEdge.BOTTOM)
            tick(50)
        }
        val fast = ShimejiPhysicsEngine().apply {
            configure(600, 1_000, 100, 0, 2f)
            restore(300f, 900f, ShimejiMotion.WALKING_RIGHT, ScreenEdge.BOTTOM)
            tick(50)
        }
        assertTrue(fast.x - 300f > slow.x - 300f)

        fast.restore(500f, 900f, ShimejiMotion.WALKING_RIGHT, ScreenEdge.BOTTOM)
        fast.configure(600, 1_000, 240, 0, 2f)
        assertTrue(fast.x <= 360f)
        assertTrue(fast.y <= 760f)
    }

    @Test fun sittingStaysPinnedUntilShakeReleaseThenFalls() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 1f)
        engine.sitAt(210f, 340f)

        repeat(100) { engine.tick(50) }
        assertEquals(ShimejiMotion.SITTING, engine.motion)
        assertEquals(210f, engine.x, 0.01f)
        assertEquals(340f, engine.y, 0.01f)

        engine.releaseFromSeat()
        assertEquals(ShimejiMotion.FALLING, engine.motion)
        engine.tick(50)
        assertTrue(engine.y > 340f)
    }

    @Test fun seatedCharacterCanBePickedUpWithoutJumpingOrFalling() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 1f)
        engine.sitAt(210f, 340f)

        engine.beginDragFromSeat()

        assertTrue(engine.isDragging)
        assertEquals(ShimejiMotion.IDLE, engine.motion)
        assertEquals(ScreenEdge.NONE, engine.edge)
        assertEquals(210f, engine.x, 0.01f)
        assertEquals(340f, engine.y, 0.01f)
        engine.tick(50)
        assertEquals(340f, engine.y, 0.01f)
    }

    @Test fun releasingPickedUpSeatInMiddleUsesExistingFallFlow() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 1f)
        engine.sitAt(210f, 340f)
        engine.beginDragFromSeat()
        engine.dragTo(250f, 300f)

        engine.endDrag()

        assertEquals(ShimejiMotion.FALLING, engine.motion)
        assertEquals(ScreenEdge.NONE, engine.edge)
        engine.tick(50)
        assertTrue(engine.y > 300f)
    }

    @Test fun releasingPickedUpSeatNearBottomUsesExistingWalkFlow() {
        val engine = ShimejiPhysicsEngine()
        engine.configure(600, 1_000, 100, 0, 1f)
        engine.sitAt(210f, 340f)
        engine.beginDragFromSeat()
        engine.dragTo(250f, 880f)

        engine.endDrag()

        assertEquals(ScreenEdge.BOTTOM, engine.edge)
        assertEquals(900f, engine.y, 0.01f)
        assertEquals(ShimejiMotion.WALKING_RIGHT, engine.motion)
    }
}
