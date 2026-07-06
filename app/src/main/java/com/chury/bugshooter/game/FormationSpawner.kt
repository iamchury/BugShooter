package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.random.Random

class FormationSpawner {
    fun createFormation(
        screenSize: Vector2,
        stage: Int,
        groupId: Int,
        nextEnemyId: () -> Int,
    ): Pair<EnemyFormation, List<MosquitoEnemy>> {
        val pattern = FormationPattern.entries.random()
        val enemyCount = Random.nextInt(
            from = GameConfig.MinEnemiesPerGroup,
            until = GameConfig.MaxEnemiesPerGroup + 1,
        )
        val speedRatio = Random.nextFloat().lerp(
            start = GameConfig.FormationSpeedMinPerScreen,
            end = GameConfig.FormationSpeedMaxPerScreen,
        )
        val speed = screenSize.y * speedRatio
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
        val radius = mosquitoRadius(screenSize)
        val spacing = screenSize.x.coerceAtMost(screenSize.y) * GameConfig.FormationSpacingRatio
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
                enemyKind = enemyKindFor(stage, index, formation.enemyCount),
                groupId = formation.groupId,
                formationPattern = formation.pattern,
                formationIndex = index,
                formationCount = formation.enemyCount,
                formationSpacing = spacing,
                basePosition = base,
                position = base,
                radius = radius,
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

    private fun enemyKindFor(stage: Int, index: Int, count: Int): EnemyKind {
        return when (stage) {
            2 -> EnemyKind.FLY
            3 -> if (index < count / 2) EnemyKind.MOSQUITO else EnemyKind.FLY
            else -> EnemyKind.MOSQUITO
        }
    }

    private fun Float.lerp(start: Float, end: Float): Float {
        return start + (end - start) * this
    }
}
