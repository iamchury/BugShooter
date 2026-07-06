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
        nextBossId: () -> Int,
    ): GameState {
        if (state.enemies.isNotEmpty() || state.boss != null) {
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

        if (state.bossQueue.isNotEmpty()) {
            val bossKind = state.bossQueue.first()
            return state.copy(
                boss = createBoss(state.screenSize, nextBossId(), bossKind),
                bossQueue = state.bossQueue.drop(1),
                currentPatternName = bossName(bossKind),
                spawnTimerSeconds = 0f,
            )
        }

        return if (state.normalGroupsSinceBoss >= GameConfig.BossEveryNormalGroups) {
            val queue = bossQueueForStage(state.currentStage)
            val bossKind = queue.first()
            state.copy(
                boss = createBoss(state.screenSize, nextBossId(), bossKind),
                bossQueue = queue.drop(1),
                normalGroupsSinceBoss = 0,
                currentPatternName = bossName(bossKind),
                spawnTimerSeconds = 0f,
            )
        } else {
            val (formation, enemies) = formationSpawner.createFormation(
                screenSize = state.screenSize,
                stage = state.currentStage,
                groupId = nextGroupId(),
                nextEnemyId = nextEnemyId,
            )
            state.copy(
                enemies = enemies,
                currentGroupId = formation.groupId,
                currentPatternName = "STAGE_${state.currentStage}_${formation.pattern.name}",
                normalGroupsSinceBoss = state.normalGroupsSinceBoss + 1,
                spawnTimerSeconds = 0f,
            )
        }
    }

    fun createMosquito(screenSize: Vector2, id: Int): MosquitoEnemy {
        return formationSpawner.createSingleMosquito(screenSize, id)
    }

    private fun createBoss(screenSize: Vector2, id: Int, kind: EnemyKind): BossMosquito {
        val radiusMultiplier = if (kind == EnemyKind.FLY) 2.45f else 2.2f
        val radius = screenSize.x.coerceAtMost(screenSize.y) * GameConfig.MosquitoRadiusRatio * radiusMultiplier
        val position = Vector2(screenSize.x / 2f, screenSize.y * 0.22f)
        return BossMosquito(
            id = id,
            enemyKind = kind,
            hp = GameConfig.BossHp,
            maxHp = GameConfig.BossHp,
            basePosition = position,
            position = position,
            radius = radius,
            screenSize = screenSize,
        )
    }

    private fun bossQueueForStage(stage: Int): List<EnemyKind> {
        return when (stage) {
            2 -> listOf(EnemyKind.FLY)
            3 -> listOf(EnemyKind.MOSQUITO, EnemyKind.FLY)
            else -> listOf(EnemyKind.MOSQUITO)
        }
    }

    private fun bossName(kind: EnemyKind): String {
        return when (kind) {
            EnemyKind.MOSQUITO -> "MOSQUITO_KING"
            EnemyKind.FLY -> "FLY_KING"
        }
    }
}
