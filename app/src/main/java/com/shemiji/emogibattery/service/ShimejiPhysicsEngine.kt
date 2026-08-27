package com.shemiji.emogibattery.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class ShimejiMotion {
    WALKING_LEFT, WALKING_RIGHT, CLIMBING_LEFT, JUMPING_LEFT_TO_RIGHT,
    CLIMBING_RIGHT, JUMPING_RIGHT_TO_LEFT, FALLING, BOUNCING,
    TOP_WALKING_LEFT, TOP_WALKING_RIGHT, IDLE,
}

enum class ScreenEdge { TOP, RIGHT, BOTTOM, LEFT, NONE }

/** Implements the complete floor, wall, cross-screen, ceiling, fall and bounce route. */
class ShimejiPhysicsEngine(@Suppress("UNUSED_PARAMETER") random: Random = Random.Default) {
    var x by mutableFloatStateOf(0f); private set
    var y by mutableFloatStateOf(0f); private set
    var motion by mutableStateOf(ShimejiMotion.WALKING_LEFT); private set
    var edge by mutableStateOf(ScreenEdge.BOTTOM); private set
    var isDragging by mutableStateOf(false); private set
    private var maxX = 0f
    private var maxY = 0f
    private var speed = 110f
    private var verticalVelocity = 0f
    private var jumpElapsedMs = 0L
    private var jumpStartY = 0f
    private var bounceElapsedMs = 0L

    fun configure(screenWidth: Int, screenHeight: Int, characterSize: Int, bottomInset: Int, speedMultiplier: Float) {
        maxX = (screenWidth - characterSize).coerceAtLeast(0).toFloat()
        maxY = (screenHeight - characterSize - bottomInset).coerceAtLeast(0).toFloat()
        speed = 110f * speedMultiplier.coerceIn(0.25f, 3f)
        x = x.coerceIn(0f, maxX); y = y.coerceIn(0f, maxY)
        if (edge != ScreenEdge.NONE) snapTo(edge)
    }

    fun restore(savedX: Float, savedY: Float, savedMotion: ShimejiMotion, savedEdge: ScreenEdge) {
        x = savedX.coerceIn(0f, maxX); y = savedY.coerceIn(0f, maxY)
        motion = savedMotion; edge = savedEdge
        if (motion == ShimejiMotion.FALLING || edge == ScreenEdge.NONE) startFalling() else snapTo(edge)
    }

    fun beginDrag() { isDragging = true; verticalVelocity = 0f }
    fun dragTo(newX: Float, newY: Float) {
        x = newX.coerceIn(0f, maxX); y = newY.coerceIn(0f, maxY); edge = ScreenEdge.NONE
    }

    fun endDrag() {
        isDragging = false
        val attachDistance = (maxX.coerceAtMost(maxY) * 0.08f).coerceIn(24f, 96f)
        val nearest = listOf(ScreenEdge.LEFT to x, ScreenEdge.RIGHT to maxX - x, ScreenEdge.TOP to y, ScreenEdge.BOTTOM to maxY - y).minBy { it.second }
        if (nearest.second > attachDistance) { startFalling(); return }
        edge = nearest.first; snapTo(edge)
        motion = when (edge) {
            ScreenEdge.BOTTOM -> if (x > maxX / 2f) ShimejiMotion.WALKING_LEFT else ShimejiMotion.WALKING_RIGHT
            ScreenEdge.LEFT -> ShimejiMotion.CLIMBING_LEFT
            ScreenEdge.RIGHT -> ShimejiMotion.CLIMBING_RIGHT
            ScreenEdge.TOP -> if (x > maxX / 2f) ShimejiMotion.TOP_WALKING_LEFT else ShimejiMotion.TOP_WALKING_RIGHT
            ScreenEdge.NONE -> ShimejiMotion.FALLING
        }
    }

    fun tick(deltaMs: Long) {
        if (isDragging || maxX <= 0f || maxY <= 0f) return
        val dtMs = deltaMs.coerceIn(0L, 50L); val distance = speed * dtMs / 1_000f
        when (motion) {
            ShimejiMotion.WALKING_LEFT -> { edge = ScreenEdge.BOTTOM; y = maxY; x -= distance; if (x <= 0f) { x = 0f; edge = ScreenEdge.LEFT; motion = ShimejiMotion.CLIMBING_LEFT } }
            ShimejiMotion.WALKING_RIGHT -> { edge = ScreenEdge.BOTTOM; y = maxY; x += distance; if (x >= maxX) { x = maxX; edge = ScreenEdge.RIGHT; motion = ShimejiMotion.CLIMBING_RIGHT } }
            ShimejiMotion.CLIMBING_LEFT -> { edge = ScreenEdge.LEFT; x = 0f; y -= distance; if (y <= maxY * 0.62f) startJump(ShimejiMotion.JUMPING_LEFT_TO_RIGHT) }
            ShimejiMotion.JUMPING_LEFT_TO_RIGHT -> updateJump(dtMs, true)
            ShimejiMotion.CLIMBING_RIGHT -> { edge = ScreenEdge.RIGHT; x = maxX; y -= distance; if (y <= maxY * 0.34f) startJump(ShimejiMotion.JUMPING_RIGHT_TO_LEFT) }
            ShimejiMotion.JUMPING_RIGHT_TO_LEFT -> updateJump(dtMs, false)
            ShimejiMotion.TOP_WALKING_LEFT -> { edge = ScreenEdge.TOP; y = 0f; x -= distance; if (x <= 0f) { x = 0f; motion = ShimejiMotion.TOP_WALKING_RIGHT } }
            ShimejiMotion.TOP_WALKING_RIGHT -> { edge = ScreenEdge.TOP; y = 0f; x += distance; if (x >= maxX) { x = maxX; startFalling() } }
            ShimejiMotion.FALLING -> updateFall(dtMs)
            ShimejiMotion.BOUNCING -> updateBounce(dtMs)
            ShimejiMotion.IDLE -> motion = ShimejiMotion.WALKING_LEFT
        }
    }

    private fun startJump(next: ShimejiMotion) { motion = next; edge = ScreenEdge.NONE; jumpElapsedMs = 0; jumpStartY = y }
    private fun updateJump(dtMs: Long, leftToRight: Boolean) {
        jumpElapsedMs += dtMs
        val duration = ((maxX / (speed * 1.7f)) * 1_000f).coerceIn(700f, 2_200f)
        val progress = (jumpElapsedMs / duration).coerceIn(0f, 1f)
        x = if (leftToRight) maxX * progress else maxX * (1f - progress)
        y = (jumpStartY - sin(progress * PI).toFloat() * maxY * 0.16f).coerceIn(0f, maxY)
        if (progress >= 1f) {
            if (leftToRight) { x = maxX; edge = ScreenEdge.RIGHT; motion = ShimejiMotion.CLIMBING_RIGHT }
            else { x = 0f; y = 0f; edge = ScreenEdge.TOP; motion = ShimejiMotion.TOP_WALKING_RIGHT }
        }
    }

    private fun startFalling() { motion = ShimejiMotion.FALLING; edge = ScreenEdge.NONE; verticalVelocity = 80f }
    private fun updateFall(dtMs: Long) {
        val dt = dtMs / 1_000f; verticalVelocity += 1_450f * dt; y += verticalVelocity * dt
        if (y >= maxY) { y = maxY; edge = ScreenEdge.BOTTOM; motion = ShimejiMotion.BOUNCING; bounceElapsedMs = 0 }
    }
    private fun updateBounce(dtMs: Long) {
        bounceElapsedMs += dtMs
        val progress = (bounceElapsedMs / 520f).coerceIn(0f, 1f)
        y = maxY - sin(progress * PI).toFloat() * (maxY * 0.07f).coerceIn(18f, 70f)
        if (progress >= 1f) { y = maxY; edge = ScreenEdge.BOTTOM; motion = if (x > maxX / 2f) ShimejiMotion.WALKING_LEFT else ShimejiMotion.WALKING_RIGHT }
    }
    private fun snapTo(target: ScreenEdge) = when (target) {
        ScreenEdge.TOP -> y = 0f; ScreenEdge.RIGHT -> x = maxX; ScreenEdge.BOTTOM -> y = maxY
        ScreenEdge.LEFT -> x = 0f; ScreenEdge.NONE -> Unit
    }
}
