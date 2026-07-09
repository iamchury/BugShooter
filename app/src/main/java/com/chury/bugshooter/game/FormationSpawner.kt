package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.random.Random

class FormationSpawner {
    fun createFormation(
        screenSize: Vector2,
        stage: Int,
        difficultyLevel: Int,
        extraEnemyCount: Int,
        groupId: Int,
        nextEnemyId: () -> Int,
    ): Pair<EnemyFormation, List<MosquitoEnemy>> {
        val pattern = FormationPattern.entries.random()
        val baseEnemyCount = Random.nextInt(
            from = GameConfig.MinEnemiesPerGroup,
            until = GameConfig.MaxEnemiesPerGroup + 1,
        )
        val enemyCount = baseEnemyCount + extraEnemyCount.coerceAtLeast(0)
        val speedRatio = Random.nextFloat().lerp(
            start = GameConfig.FormationSpeedMinPerScreen,
            end = GameConfig.FormationSpeedMaxPerScreen,
        )
        val speed = screenSize.y * speedRatio * difficultySpeedMultiplier(difficultyLevel)
        val tunedSpeed = when (pattern) {
            FormationPattern.ZIGZAG -> speed * 1.18f
            else -> speed
        }
        val formation = EnemyFormation(
            groupId = groupId,
            pattern = pattern,
            enemyCount = enemyCount,
            speed = tunedSpeed,
        )
        return formation to createEnemies(screenSize, stage, formation, nextEnemyId)
    }

    fun createSingleMosquito(
        screenSize: Vector2,
        id: Int,
        groupId: Int = 0,
    ): MosquitoEnemy {
        val radius = mosquitoRadius(screenSize)
        return MosquitoEnemy(
            id = id,
            enemyKind = EnemyKind.MOSQUITO,
            groupId = groupId,
            formationPattern = FormationPattern.STRAIGHT_DOWN,
            formationIndex = 0,
            formationCount = 1,
            formationSpacing = 0f,
            basePosition = Vector2(screenSize.x / 2f, -radius),
            position = Vector2(screenSize.x / 2f, -radius),
            radius = radius,
            speed = screenSize.y * GameConfig.MosquitoSpeedPerScreen,
            screenSize = screenSize,
        )
    }

    private fun createEnemies(
        screenSize: Vector2,
        stage: Int,
        formation: EnemyFormation,
        nextEnemyId: () -> Int,
    ): List<MosquitoEnemy> {
        val kinds = List(formation.enemyCount) { index ->
            enemyKindFor(stage, index, formation.enemyCount)
        }
        val radius = kinds.maxOf { enemyRadius(screenSize, it) }
        val defaultSpacing = screenSize.x.coerceAtMost(screenSize.y) * GameConfig.FormationSpacingRatio
        val maxSpacing = (screenSize.x - radius * 2f) / formation.enemyCount.coerceAtLeast(1)
        val spacing = defaultSpacing.coerceAtMost(maxSpacing)
        val rowHalfWidth = (formation.enemyCount - 1) * spacing / 2f
        val centerX = Random.nextFloat()
            .lerp(start = radius + rowHalfWidth, end = screenSize.x - radius - rowHalfWidth)

        return List(formation.enemyCount) { index ->
            val base = basePositionFor(
                index = index,
                count = formation.enemyCount,
                centerX = centerX,
                spacing = spacing,
                radius = radius,
            )
            MosquitoEnemy(
                id = nextEnemyId(),
                enemyKind = kinds[index],
                groupId = formation.groupId,
                formationPattern = formation.pattern,
                formationIndex = index,
                formationCount = formation.enemyCount,
                formationSpacing = spacing,
                basePosition = base,
                position = base,
                radius = enemyRadius(screenSize, kinds[index]),
                speed = formation.speed,
                screenSize = screenSize,
            )
        }
    }

    private fun basePositionFor(
        index: Int,
        count: Int,
        centerX: Float,
        spacing: Float,
        radius: Float,
    ): Vector2 {
        val centeredIndex = index - (count - 1) / 2f
        val topY = -radius * 2f
        return Vector2(centerX + centeredIndex * spacing, topY)
    }

    private fun mosquitoRadius(screenSize: Vector2): Float {
        return screenSize.x.coerceAtMost(screenSize.y) * GameConfig.MosquitoRadiusRatio
    }

    private fun enemyRadius(screenSize: Vector2, kind: EnemyKind): Float {
        val base = mosquitoRadius(screenSize)
        return base * when (kind) {
            EnemyKind.MOSQUITO -> GameConfig.MosquitoScale
            EnemyKind.FLY -> GameConfig.FlyScale
            EnemyKind.HONEY_BEE -> GameConfig.HoneyBeeScale
            EnemyKind.WASP -> GameConfig.WaspScale
            EnemyKind.WHITE_BUTTERFLY -> GameConfig.WhiteButterflyScale
            EnemyKind.SWALLOWTAIL_BUTTERFLY -> GameConfig.SwallowtailButterflyScale
            EnemyKind.STAG_BEETLE -> GameConfig.StagBeetleScale
        }
    }

    private fun enemyKindFor(stage: Int, index: Int, count: Int): EnemyKind {
        return when (stage) {
            2 -> EnemyKind.FLY
            3 -> if (index < count / 2) EnemyKind.MOSQUITO else EnemyKind.FLY
            4 -> EnemyKind.HONEY_BEE
            5 -> EnemyKind.WASP
            6 -> if (index < count / 2) EnemyKind.HONEY_BEE else EnemyKind.WASP
            7 -> EnemyKind.WHITE_BUTTERFLY
            8 -> EnemyKind.SWALLOWTAIL_BUTTERFLY
            9 -> if (index < count / 2) EnemyKind.WHITE_BUTTERFLY else EnemyKind.SWALLOWTAIL_BUTTERFLY
            10 -> EnemyKind.STAG_BEETLE
            else -> EnemyKind.MOSQUITO
        }
    }

    private fun Float.lerp(start: Float, end: Float): Float {
        return start + (end - start) * this
    }

    private fun difficultySpeedMultiplier(difficultyLevel: Int): Float {
        val bonus = ((difficultyLevel - 1).coerceAtLeast(0) * GameConfig.DifficultySpeedStep)
            .coerceAtMost(GameConfig.MaxDifficultySpeedBonus)
        return 1f + bonus
    }
}
