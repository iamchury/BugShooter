package com.chury.bugshooter.engine

data class Vector2(
    val x: Float,
    val y: Float,
) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vector2(x * scale, y * scale)

    companion object {
        val Zero = Vector2(0f, 0f)
    }
}
