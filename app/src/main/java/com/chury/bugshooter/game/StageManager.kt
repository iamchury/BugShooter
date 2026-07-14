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
        if (state.enemies.isNotEmpty() || state.bosses.isNotEmpty()) {
            return state.copy(spawnTimerSeconds = 0f)
        }

        // Bosses only unlock after ten groups were fully shot down, not merely passed.
        val completedGroup = state.currentGroupId != null
        val countedGroups = if (completedGroup && !state.currentGroupHadMiss) {
            state.normalGroupsSinceBoss + 1
        } else {
            state.normalGroupsSinceBoss
        }
        val baseState = if (completedGroup) {
            state.copy(
                currentGroupId = null,
                currentGroupHadMiss = false,
                normalGroupsSinceBoss = countedGroups,
            )
        } else {
            state
        }

        val nextTimer = baseState.spawnTimerSeconds + deltaSeconds
        val shouldSpawnImmediately = state.currentGroupId == null
        if (!shouldSpawnImmediately && nextTimer < GameConfig.NextGroupDelaySeconds) {
            return baseState.copy(
                spawnTimerSeconds = nextTimer,
                currentPatternName = "NEXT_GROUP_WAIT",
            )
        }

        return if (baseState.normalGroupsSinceBoss >= GameConfig.BossEveryNormalGroups && baseState.carryOverEnemies == 0) {
            val bossKinds = bossKindsForStage(baseState.currentStage)
            val bosses = bossKinds.mapIndexed { index, kind ->
                createBoss(
                    screenSize = baseState.screenSize,
                    id = nextBossId(),
                    kind = kind,
                    stage = baseState.currentStage,
                    difficultyLevel = baseState.difficultyLevel,
                    index = index,
                    total = bossKinds.size,
                )
            }
            baseState.copy(
                bosses = bosses,
                normalGroupsSinceBoss = 0,
                currentPatternName = bossKinds.joinToString("+") { bossName(it) },
                spawnTimerSeconds = 0f,
            )
        } else {
            val (formation, enemies) = formationSpawner.createFormation(
                screenSize = baseState.screenSize,
                stage = baseState.currentStage,
                difficultyLevel = baseState.difficultyLevel,
                extraEnemyCount = baseState.carryOverEnemies,
                groupId = nextGroupId(),
                nextEnemyId = nextEnemyId,
            )
            baseState.copy(
                enemies = enemies,
                currentGroupId = formation.groupId,
                currentGroupHadMiss = false,
                currentPatternName = "STAGE_${baseState.currentStage}_${formation.pattern.name}",
                carryOverEnemies = 0,
                spawnTimerSeconds = 0f,
            )
        }
    }

    fun createMosquito(screenSize: Vector2, id: Int): MosquitoEnemy {
        return formationSpawner.createSingleMosquito(screenSize, id)
    }

    private fun createBoss(
        screenSize: Vector2,
        id: Int,
        kind: EnemyKind,
        stage: Int,
        difficultyLevel: Int,
        index: Int,
        total: Int,
    ): BossMosquito {
        val baseRadiusMultiplier = when (kind) {
            EnemyKind.MOSQUITO -> 2.65f
            EnemyKind.FLY -> 3.05f
            EnemyKind.HONEY_BEE -> 2.95f
            EnemyKind.WASP -> 3.35f
            EnemyKind.WHITE_BUTTERFLY -> 3.35f
            EnemyKind.SWALLOWTAIL_BUTTERFLY -> 3.75f
            EnemyKind.STAG_BEETLE -> 4.15f
        }
        val radiusMultiplier = baseRadiusMultiplier
        val radius = screenSize.x.coerceAtMost(screenSize.y) * GameConfig.MosquitoRadiusRatio * radiusMultiplier
        val offset = (index - (total - 1) / 2f) * screenSize.x * 0.34f
        val position = Vector2((screenSize.x / 2f + offset).coerceIn(radius, screenSize.x - radius), screenSize.y * 0.2f)
        val bossHp = bossHpFor(stage, kind, index, total)
        return BossMosquito(
            id = id,
            enemyKind = kind,
            hp = bossHp,
            maxHp = bossHp,
            basePosition = position,
            position = position,
            radius = radius,
            screenSize = screenSize,
            speed = screenSize.y * GameConfig.BossDownSpeedPerScreen * difficultySpeedMultiplier(difficultyLevel),
        )
    }

    private fun bossKindsForStage(stage: Int): List<EnemyKind> {
        return when (stage) {
            2 -> listOf(EnemyKind.FLY)
            3 -> listOf(EnemyKind.MOSQUITO, EnemyKind.FLY)
            4 -> listOf(EnemyKind.HONEY_BEE)
            5 -> listOf(EnemyKind.WASP)
            6 -> listOf(EnemyKind.HONEY_BEE, EnemyKind.WASP)
            7 -> listOf(EnemyKind.WHITE_BUTTERFLY)
            8 -> listOf(EnemyKind.SWALLOWTAIL_BUTTERFLY)
            9 -> listOf(EnemyKind.WHITE_BUTTERFLY, EnemyKind.SWALLOWTAIL_BUTTERFLY)
            10 -> listOf(EnemyKind.STAG_BEETLE, EnemyKind.STAG_BEETLE)
            else -> listOf(EnemyKind.MOSQUITO)
        }
    }

    private fun bossHpFor(stage: Int, kind: EnemyKind, index: Int, total: Int): Int {
        return when (stage) {
            1 -> 25
            2 -> 35
            3 -> 50
            4 -> 65
            5 -> 80
            6 -> 100
            7 -> 125
            8 -> 150
            9 -> 180
            else -> 220
        }
    }

    private fun bossName(kind: EnemyKind): String {
        return when (kind) {
            EnemyKind.MOSQUITO -> "MOSQUITO_KING"
            EnemyKind.FLY -> "FLY_KING"
            EnemyKind.HONEY_BEE -> "HONEY_BEE_KING"
            EnemyKind.WASP -> "WASP_KING"
            EnemyKind.WHITE_BUTTERFLY -> "WHITE_BUTTERFLY_KING"
            EnemyKind.SWALLOWTAIL_BUTTERFLY -> "SWALLOWTAIL_KING"
            EnemyKind.STAG_BEETLE -> "STAG_BEETLE_KING"
        }
    }

    private fun difficultySpeedMultiplier(difficultyLevel: Int): Float {
        val bonus = ((difficultyLevel - 1).coerceAtLeast(0) * GameConfig.DifficultySpeedStep)
            .coerceAtMost(GameConfig.MaxDifficultySpeedBonus)
        return 1f + bonus
    }
}
