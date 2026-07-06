package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.random.Random

class StageManager {
    fun update(
        state: GameState,
        deltaSeconds: Float,
        nextEnemyId: () -> Int,
    ): GameState {
        val nextTimer = state.spawnTimerSeconds + deltaSeconds
        return if (nextTimer >= GameConfig.MosquitoSpawnIntervalSeconds) {
            val mosquito = createMosquito(state.screenSize, nextEnemyId())
            state.copy(
                enemies = state.enemies + mosquito,
                spawnTimerSeconds = nextTimer - GameConfig.MosquitoSpawnIntervalSeconds,
            )
        } else {
            state.copy(spawnTimerSeconds = nextTimer)
        }
    }

    fun createMosquito(screenSize: Vector2, id: Int): MosquitoEnemy {
        val radius = screenSize.x.coerceAtMost(screenSize.y) * GameConfig.MosquitoRadiusRatio
        val x = Random.nextFloat() * (screenSize.x - radius * 2f) + radius
        return MosquitoEnemy(
            id = id,
            position = Vector2(x = x, y = -radius),
            radius = radius,
            speed = screenSize.y * GameConfig.MosquitoSpeedPerScreen,
        )
    }
}
