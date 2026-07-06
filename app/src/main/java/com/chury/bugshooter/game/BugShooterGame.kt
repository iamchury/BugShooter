package com.chury.bugshooter.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chury.bugshooter.engine.Collision
import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.random.Random

class BugShooterGame {
    var state by mutableStateOf(GameState())
        private set

    private val stageManager = StageManager()
    private var nextBulletId = 1
    private var nextEnemyId = 1
    private var nextGroupId = 1
    private var nextExplosionId = 1
    private var nextPowerUpId = 1
    private var nextEnemyBulletId = 1
    private var nextBossId = 1

    fun setScreenSize(screenSize: Vector2) {
        if (screenSize.x <= 0f || screenSize.y <= 0f) return

        if (state.screenSize == Vector2.Zero) {
            reset(screenSize)
            return
        }

        // Keep the simulation responsive if the Canvas size changes.
        val playerSize = playerSizeFor(screenSize)
        val playerY = screenSize.y * GameConfig.PlayerCenterYRatio
        val player = state.player
            .copy(size = playerSize, position = state.player.position.copy(y = playerY))
            .moveHorizontally(deltaX = 0f, screenWidth = screenSize.x)
        state = state.copy(screenSize = screenSize, player = player)
    }

    fun reset(screenSize: Vector2 = state.screenSize) {
        if (screenSize.x <= 0f || screenSize.y <= 0f) return

        nextBulletId = 1
        nextEnemyId = 1
        nextGroupId = 1
        nextExplosionId = 1
        nextPowerUpId = 1
        nextEnemyBulletId = 1
        nextBossId = 1
        val playerSize = playerSizeFor(screenSize)
        state = GameState(
            screenSize = screenSize,
            player = Player(
                position = Vector2(screenSize.x / 2f, screenSize.y * GameConfig.PlayerCenterYRatio),
                size = playerSize,
            ),
        )
    }

    fun movePlayerBy(deltaX: Float) {
        if (state.isGameOver || state.screenSize == Vector2.Zero) return
        state = state.copy(player = state.player.moveHorizontally(deltaX, state.screenSize.x))
    }

    fun fireBullet() {
        if (state.isGameOver || state.fireCooldownSeconds > 0f || state.screenSize == Vector2.Zero) return
        if (state.bullets.size >= GameConfig.MaxPlayerBullets) return

        val bulletRadius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.BulletRadiusRatio
        val bulletY = state.player.position.y - state.player.size.y / 2f
        val bulletSpeed = state.screenSize.y * GameConfig.BulletSpeedPerScreen
        val bullets = if (state.doubleShotSeconds > 0f) {
            listOf(
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x - state.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x + state.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
            )
        } else {
            listOf(Bullet(nextBulletId++, state.player.position.copy(y = bulletY), bulletRadius, bulletSpeed))
        }
        val cooldown = if (state.rapidFireSeconds > 0f) {
            GameConfig.RapidFireCooldownSeconds
        } else {
            GameConfig.FireCooldownSeconds
        }
        state = state.copy(
            bullets = (state.bullets + bullets).takeLast(GameConfig.MaxPlayerBullets),
            fireCooldownSeconds = cooldown,
        )
    }

    fun update(deltaSeconds: Float) {
        if (state.screenSize == Vector2.Zero || state.isGameOver) return

        val movedBullets = state.bullets
            .map { it.update(deltaSeconds) }
            .filter { it.position.y + it.radius >= 0f }
        val movedEnemies = state.enemies.map { it.update(deltaSeconds) }
        val movedBoss = state.boss?.update(deltaSeconds)
        val movedPowerUps = state.powerUps
            .map { it.update(deltaSeconds) }
            .filter { it.position.y - it.radius <= state.screenSize.y }
        val movedEnemyBullets = state.enemyBullets
            .map { it.update(deltaSeconds) }
            .filter { it.position.y - it.radius <= state.screenSize.y }
        val activeExplosions = state.explosions
            .map { it.update(deltaSeconds) }
            .filterNot { it.isFinished }
        val nextComboTimer = (state.comboTimerSeconds - deltaSeconds).coerceAtLeast(0f)
        val nextCombo = if (nextComboTimer <= 0f) 0 else state.combo

        val timedState = state.copy(
            bullets = movedBullets,
            enemies = movedEnemies,
            boss = movedBoss,
            powerUps = movedPowerUps,
            enemyBullets = movedEnemyBullets,
            explosions = activeExplosions,
            combo = nextCombo,
            comboTimerSeconds = nextComboTimer,
            doubleShotSeconds = (state.doubleShotSeconds - deltaSeconds).coerceAtLeast(0f),
            rapidFireSeconds = (state.rapidFireSeconds - deltaSeconds).coerceAtLeast(0f),
            fireCooldownSeconds = (state.fireCooldownSeconds - deltaSeconds).coerceAtLeast(0f),
            enemyFireTimerSeconds = (state.enemyFireTimerSeconds - deltaSeconds).coerceAtLeast(0f),
            bossFireTimerSeconds = (state.bossFireTimerSeconds - deltaSeconds).coerceAtLeast(0f),
            playerHitFlashSeconds = (state.playerHitFlashSeconds - deltaSeconds).coerceAtLeast(0f),
        )

        val afterBulletHits = resolveBossCollisions(
            resolveBulletEnemyCollisions(timedState),
        )
        val afterPowerUps = resolvePowerUpCollection(afterBulletHits)
        val afterEnemyFire = updateEnemyFire(afterPowerUps)
        val afterPlayerDamage = resolvePlayerDamage(afterEnemyFire)

        val aliveState = afterPlayerDamage.copy(isGameOver = afterPlayerDamage.lives <= 0)

        state = if (aliveState.isGameOver) {
            aliveState
        } else {
            stageManager.update(
                state = aliveState,
                deltaSeconds = deltaSeconds,
                nextEnemyId = { nextEnemyId++ },
                nextGroupId = { nextGroupId++ },
                nextBossId = { nextBossId++ },
            )
        }
    }

    fun spawnMosquitoForDebug() {
        if (state.screenSize == Vector2.Zero || state.isGameOver) return
        val mosquito = stageManager.createMosquito(state.screenSize, nextEnemyId++)
        state = state.copy(enemies = state.enemies + mosquito)
    }

    private fun resolveBulletEnemyCollisions(input: GameState): GameState {
        val hitBulletIds = mutableSetOf<Int>()
        val hitEnemyIds = mutableSetOf<Int>()
        val newExplosions = mutableListOf<Explosion>()
        val newPowerUps = mutableListOf<PowerUp>()

        // Collision resolution is centralized here so later stages can reuse it for new enemy types.
        input.bullets.forEach { bullet ->
            input.enemies.firstOrNull { enemy ->
                enemy.id !in hitEnemyIds && Collision.circlesIntersect(bullet, enemy)
            }?.let { enemy ->
                hitBulletIds += bullet.id
                hitEnemyIds += enemy.id
                newExplosions += Explosion(
                    id = nextExplosionId++,
                    position = enemy.position,
                    maxRadius = enemy.radius * 2.4f,
                )
                maybeCreatePowerUp(enemy)?.let { newPowerUps += it }
            }
        }

        val killedEnemies = input.enemies.filter { it.id in hitEnemyIds }
        val scoreGain = killedEnemies.sumOf { enemyScore(it) } * ComboSystem.multiplier(input.combo)
        val nextCombo = input.combo + killedEnemies.size

        return input.copy(
            bullets = input.bullets.filterNot { it.id in hitBulletIds },
            enemies = input.enemies.filterNot { it.id in hitEnemyIds },
            powerUps = input.powerUps + newPowerUps,
            explosions = input.explosions + newExplosions,
            score = input.score + scoreGain,
            combo = nextCombo,
            comboTimerSeconds = if (killedEnemies.isEmpty()) input.comboTimerSeconds else ComboSystem.refreshedTimer(),
        )
    }

    private fun resolveBossCollisions(input: GameState): GameState {
        val boss = input.boss ?: return input
        val hitBullets = input.bullets.filter { Collision.circlesIntersect(it, boss) }
        if (hitBullets.isEmpty()) return input

        val damagedBoss = boss.damage(hitBullets.size)
        val hitBulletIds = hitBullets.mapTo(mutableSetOf()) { it.id }
        val sparks = hitBullets.map {
            Explosion(nextExplosionId++, it.position, boss.radius * 0.5f)
        }

        if (damagedBoss.hp > 0) {
            return input.copy(
                bullets = input.bullets.filterNot { it.id in hitBulletIds },
                boss = damagedBoss,
                explosions = input.explosions + sparks,
            )
        }

        return input.copy(
            bullets = input.bullets.filterNot { it.id in hitBulletIds },
            boss = null,
            enemyBullets = emptyList(),
            explosions = input.explosions + sparks + Explosion(nextExplosionId++, boss.position, boss.radius * 3f),
            score = input.score + GameConfig.BossScoreBonus * ComboSystem.multiplier(input.combo),
            combo = input.combo + GameConfig.BossHp,
            comboTimerSeconds = ComboSystem.refreshedTimer(),
            currentPatternName = "${boss.enemyKind.name}_KING_DEFEATED",
            currentStage = nextStageAfterBoss(input.currentStage, boss.enemyKind, input.bossQueue),
            normalGroupsSinceBoss = if (input.bossQueue.isEmpty()) 0 else input.normalGroupsSinceBoss,
        )
    }

    private fun resolvePowerUpCollection(input: GameState): GameState {
        val collected = input.powerUps.filter { Collision.circlesIntersect(input.player, it) }
        if (collected.isEmpty()) return input

        var next = input.copy(powerUps = input.powerUps.filterNot { powerUp -> collected.any { it.id == powerUp.id } })
        collected.forEach { powerUp ->
            next = when (powerUp.type) {
                PowerUpType.DOUBLE_SHOT -> next.copy(doubleShotSeconds = GameConfig.PowerUpDurationSeconds)
                PowerUpType.RAPID_FIRE -> next.copy(rapidFireSeconds = GameConfig.PowerUpDurationSeconds)
                PowerUpType.SHIELD -> next.copy(shieldCharges = next.shieldCharges + 1)
                PowerUpType.HEAL -> next.copy(lives = (next.lives + 1).coerceAtMost(GameConfig.InitialLives))
                PowerUpType.BOMB -> activateBomb(next)
            }
        }
        return next
    }

    private fun activateBomb(input: GameState): GameState {
        val blastExplosions = input.enemies.map {
            Explosion(nextExplosionId++, it.position, it.radius * 2.8f)
        }
        return input.copy(
            enemies = emptyList(),
            enemyBullets = emptyList(),
            explosions = input.explosions + blastExplosions,
        )
    }

    private fun updateEnemyFire(input: GameState): GameState {
        var next = input
        if (
            next.currentPatternName.contains(FormationPattern.SPIRAL_DOWN.name) &&
            next.enemies.isNotEmpty() &&
            next.enemyFireTimerSeconds <= 0f
        ) {
            val shooter = next.enemies.random()
            next = next.copy(
                enemyBullets = next.enemyBullets + createEnemyBullet(shooter.position),
                enemyFireTimerSeconds = GameConfig.SpiralEnemyFireIntervalSeconds,
            )
        }

        val boss = next.boss
        if (boss != null && next.bossFireTimerSeconds <= 0f) {
            next = next.copy(
                enemyBullets = next.enemyBullets + createEnemyBullet(boss.position.copy(y = boss.position.y + boss.radius)),
                bossFireTimerSeconds = GameConfig.BossFireIntervalSeconds,
            )
        }
        return next
    }

    private fun createEnemyBullet(position: Vector2): EnemyBullet {
        val radius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.EnemyBulletRadiusRatio
        return EnemyBullet(
            id = nextEnemyBulletId++,
            position = position,
            radius = radius,
            speed = state.screenSize.y * GameConfig.EnemyBulletSpeedPerScreen,
        )
    }

    private fun maybeCreatePowerUp(enemy: Enemy): PowerUp? {
        if (Random.nextFloat() > GameConfig.PowerUpDropChance) return null
        val radius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.PowerUpRadiusRatio
        return PowerUp(
            id = nextPowerUpId++,
            type = PowerUpType.entries.random(),
            position = enemy.position,
            radius = radius,
            speed = state.screenSize.y * GameConfig.PowerUpSpeedPerScreen,
        )
    }

    private fun enemyScore(enemy: Enemy): Int {
        val kindBonus = if (enemy.enemyKind == EnemyKind.FLY) 2 else 0
        return when (enemy.formationPattern) {
            FormationPattern.LINE_HORIZONTAL -> GameConfig.LineHorizontalScore + kindBonus
            FormationPattern.CIRCLE_LEFT,
            FormationPattern.CIRCLE_RIGHT -> GameConfig.CirclePatternScore + kindBonus
            else -> GameConfig.MosquitoBaseScore + kindBonus
        }
    }

    private fun nextStageAfterBoss(
        currentStage: Int,
        defeatedKind: EnemyKind,
        remainingBossQueue: List<EnemyKind>,
    ): Int {
        if (remainingBossQueue.isNotEmpty()) return currentStage
        return when {
            currentStage == 1 && defeatedKind == EnemyKind.MOSQUITO -> 2
            currentStage == 2 && defeatedKind == EnemyKind.FLY -> 3
            else -> currentStage
        }
    }

    private fun resolvePlayerDamage(input: GameState): GameState {
        val escapedEnemyIds = input.enemies
            .filter { it.position.y - it.radius > input.screenSize.y }
            .mapTo(mutableSetOf()) { it.id }
        val contactEnemyIds = input.enemies
            .filter { it.id !in escapedEnemyIds && Collision.circlesIntersect(input.player, it) }
            .mapTo(mutableSetOf()) { it.id }
        val enemyBulletIds = input.enemyBullets
            .filter { Collision.circlesIntersect(input.player, it) }
            .mapTo(mutableSetOf()) { it.id }

        if (escapedEnemyIds.isEmpty() && contactEnemyIds.isEmpty() && enemyBulletIds.isEmpty()) return input

        val stateAfterMisses = input.copy(
            enemies = input.enemies.filterNot { it.id in escapedEnemyIds || it.id in contactEnemyIds },
            enemyBullets = input.enemyBullets.filterNot { it.id in enemyBulletIds },
            misses = input.misses + escapedEnemyIds.size,
        )

        val damageEvents = contactEnemyIds.size + enemyBulletIds.size
        if (damageEvents <= 0 || input.playerHitFlashSeconds > 0f) {
            return stateAfterMisses
        }

        return applyPlayerHit(stateAfterMisses)
    }

    private fun applyPlayerHit(input: GameState): GameState {
        if (input.shieldCharges > 0) {
            return input.copy(
                shieldCharges = input.shieldCharges - 1,
                playerHitFlashSeconds = GameConfig.PlayerHitFlashSeconds,
                combo = 0,
                comboTimerSeconds = 0f,
            )
        }

        val lives = (input.lives - 1).coerceAtLeast(0)
        return input.copy(
            lives = lives,
            hits = input.hits + 1,
            score = (input.score - GameConfig.MosquitoBaseScore).coerceAtLeast(0),
            combo = 0,
            comboTimerSeconds = 0f,
            playerHitFlashSeconds = GameConfig.PlayerHitFlashSeconds,
            isGameOver = lives <= 0,
        )
    }

    private fun playerSizeFor(screenSize: Vector2): Vector2 {
        return Vector2(
            x = screenSize.x * GameConfig.PlayerWidthRatio,
            y = screenSize.y * GameConfig.PlayerHeightRatio,
        )
    }
}
