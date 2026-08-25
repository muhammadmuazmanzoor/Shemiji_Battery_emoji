package com.shemiji.emogibattery.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

enum class ShimejiMotion { WALKING_LEFT, WALKING_RIGHT, IDLE }
enum class ScreenEdge { TOP, RIGHT, BOTTOM, LEFT }

/** Moves the character around the complete usable screen perimeter in physical pixels. */
class ShimejiPhysicsEngine(private val random: Random = Random.Default) {
    var x by mutableFloatStateOf(0f); private set
    var y by mutableFloatStateOf(0f); private set
    var motion by mutableStateOf(ShimejiMotion.WALKING_RIGHT); private set
    var edge by mutableStateOf(ScreenEdge.BOTTOM); private set
    var isDragging by mutableStateOf(false); private set
    private var maxX = 0f
    private var maxY = 0f
    private var speed = 110f
    private var idleRemainingMs = 0L

    fun configure(screenWidth: Int, screenHeight: Int, characterSize: Int, bottomInset: Int, speedMultiplier: Float) {
        maxX = (screenWidth - characterSize).coerceAtLeast(0).toFloat()
        maxY = (screenHeight - characterSize - bottomInset).coerceAtLeast(0).toFloat()
        speed = 110f * speedMultiplier.coerceIn(0.25f, 3f)
        x = x.coerceIn(0f, maxX); y = y.coerceIn(0f, maxY); snapTo(edge)
    }

    fun restore(savedX: Float, savedY: Float, savedMotion: ShimejiMotion, savedEdge: ScreenEdge) {
        x = savedX.coerceIn(0f, maxX); y = savedY.coerceIn(0f, maxY)
        edge = savedEdge; motion = savedMotion
        idleRemainingMs = if (motion == ShimejiMotion.IDLE) 1_000L else 0L
        snapTo(edge)
    }

    fun beginDrag() { isDragging = true }
    fun dragTo(newX: Float, newY: Float) { x = newX.coerceIn(0f, maxX); y = newY.coerceIn(0f, maxY) }

    fun endDrag() {
        isDragging = false
        edge = listOf(ScreenEdge.LEFT to x, ScreenEdge.RIGHT to maxX - x, ScreenEdge.TOP to y, ScreenEdge.BOTTOM to maxY - y)
            .minBy { it.second }.first
        snapTo(edge)
        motion = if ((if (edge == ScreenEdge.TOP || edge == ScreenEdge.BOTTOM) x else y) >
            (if (edge == ScreenEdge.TOP || edge == ScreenEdge.BOTTOM) maxX else maxY) / 2f) {
            ShimejiMotion.WALKING_LEFT
        } else ShimejiMotion.WALKING_RIGHT
    }

    fun tick(deltaMs: Long) {
        if (isDragging || maxX <= 0f || maxY <= 0f) return
        val dt = deltaMs.coerceIn(0L, 50L)
        if (motion == ShimejiMotion.IDLE) {
            idleRemainingMs -= dt
            if (idleRemainingMs <= 0) motion = ShimejiMotion.WALKING_RIGHT
            return
        }
        moveClockwise(speed * dt / 1_000f * if (motion == ShimejiMotion.WALKING_RIGHT) 1f else -1f)
    }

    private fun moveClockwise(distance: Float) {
        var p = when (edge) {
            ScreenEdge.TOP -> x
            ScreenEdge.RIGHT -> maxX + y
            ScreenEdge.BOTTOM -> maxX + maxY + (maxX - x)
            ScreenEdge.LEFT -> maxX * 2f + maxY + (maxY - y)
        } + distance
        val length = 2f * (maxX + maxY)
        p = ((p % length) + length) % length
        edge = when { p <= maxX -> ScreenEdge.TOP; p <= maxX + maxY -> ScreenEdge.RIGHT; p <= maxX * 2f + maxY -> ScreenEdge.BOTTOM; else -> ScreenEdge.LEFT }
        when (edge) {
            ScreenEdge.TOP -> { x = p; y = 0f }
            ScreenEdge.RIGHT -> { x = maxX; y = p - maxX }
            ScreenEdge.BOTTOM -> { x = maxX - (p - maxX - maxY); y = maxY }
            ScreenEdge.LEFT -> { x = 0f; y = maxY - (p - maxX * 2f - maxY) }
        }
        if ((x == 0f || x == maxX) && (y == 0f || y == maxY) && random.nextFloat() < 0.002f) {
            motion = ShimejiMotion.IDLE; idleRemainingMs = random.nextLong(700L, 1_800L)
        }
    }

    private fun snapTo(target: ScreenEdge) = when (target) {
        ScreenEdge.TOP -> y = 0f; ScreenEdge.RIGHT -> x = maxX
        ScreenEdge.BOTTOM -> y = maxY; ScreenEdge.LEFT -> x = 0f
    }
}
