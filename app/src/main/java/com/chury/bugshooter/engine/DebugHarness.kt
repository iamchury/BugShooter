package com.chury.bugshooter.engine

import com.chury.bugshooter.game.BugShooterGame
import com.chury.bugshooter.game.Bullet
import com.chury.bugshooter.game.MosquitoEnemy

object DebugHarness {
    fun resetGame(game: BugShooterGame, screenSize: Vector2) {
        game.reset(screenSize)
    }

    fun spawnOneMosquito(game: BugShooterGame) {
        game.spawnMosquitoForDebug()
    }

    fun simulateOneUpdateStep(game: BugShooterGame, deltaSeconds: Float = 1f / 60f) {
        game.update(deltaSeconds)
    }

    fun verifyCollisionLogic(): Boolean {
        val bullet = Bullet(
            id = 1,
            position = Vector2(100f, 100f),
            radius = 8f,
            speed = 0f,
        )
        val mosquito = MosquitoEnemy(
            id = 2,
            position = Vector2(106f, 100f),
            radius = 10f,
            speed = 0f,
        )
        return Collision.circlesIntersect(bullet, mosquito)
    }
}
