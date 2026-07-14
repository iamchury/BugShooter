package com.chury.bugshooter.engine

object Collision {
    fun circlesIntersect(a: GameObject, b: GameObject): Boolean {
        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        val combinedRadius = a.radius + b.radius
        return dx * dx + dy * dy <= combinedRadius * combinedRadius
    }
}
