package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2

class StageManager {
    private val formationSpawner = FormationSpawner()

    fun update(
        state: GameState,
        deltaSeconds: Float,
        nextEnemyId: () -> Int,
        nextGroupId: () -> Int,
    ): GameState {
        if (state.enemies.isNotEmpty()) {
            return state.copy(spawnTimerSeconds = 0f)
        }

        val nextTimer = state.spawnTimerSeconds + deltaSeconds
        val shouldSpawnImmediately = state.currentGroupId == null
        if (!shouldSpawnImmediately && nextTimer < GameConfig.NextGroupDelaySeconds) {
            return state.copy(
                spawnTimerSeconds = nextTimer,
                currentPatternName = "NEXT_GROUP_WAIT",
            )
        }

        val (formation, enemies) = formationSpawner.createFormation(
            screenSize = state.screenSize,
            groupId = nextGroupId(),
            nextEnemyId = nextEnemyId,
        )
        return state.copy(
            enemies = enemies,
            currentGroupId = formation.groupId,
            currentPatternName = formation.pattern.name,
            spawnTimerSeconds = 0f,
        )
    }

    fun createMosquito(screenSize: Vector2, id: Int): MosquitoEnemy {
        return formationSpawner.createSingleMosquito(screenSize, id)
    }
}
