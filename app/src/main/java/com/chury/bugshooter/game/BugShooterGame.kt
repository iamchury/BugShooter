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

    private val persistentPowerUpSeconds = Float.POSITIVE_INFINITY
    private val stageManager = StageManager()
    private var nextBulletId = 1
    private var nextEnemyId = 1
    private var nextGroupId = 1
    private var nextExplosionId = 1
    private var nextPowerUpId = 1
    private var nextEnemyBulletId = 1
    private var nextBossId = 1
    private val normalPowerUpTypes = listOf(
        PowerUpType.SPEED_2,
        PowerUpType.SPEED_4,
        PowerUpType.DOUBLE_SHOT,
        PowerUpType.SHIELD_1,
        PowerUpType.SHIELD_2,
    )

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
            .moveTo(state.player.position.copy(y = playerY), screenSize, playerMinYFor(screenSize))
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

    fun startTouch(position: Vector2) {
        if (state.screenSize == Vector2.Zero) return
        state = state.copy(
            isTouching = true,
            player = state.player.moveTo(position, state.screenSize, playerMinYFor(state.screenSize)),
        )
    }

    fun moveTouch(position: Vector2) {
        if (state.screenSize == Vector2.Zero) return
        state = state.copy(
            isTouching = true,
            player = state.player.moveTo(position, state.screenSize, playerMinYFor(state.screenSize)),
        )
    }

    fun endTouch() {
        state = state.copy(isTouching = false)
    }

    fun fireBullet() {
        if (state.isGameOver || state.fireCooldownSeconds > 0f || state.screenSize == Vector2.Zero) return
        if (state.bullets.size >= GameConfig.MaxPlayerBullets) return

        val powerScale = playerBulletPowerScale(state)
        val bulletRadius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.BulletRadiusRatio * powerScale
        val bulletY = state.player.position.y - state.player.size.y / 2f
        val bulletSpeed = state.screenSize.y * GameConfig.BulletSpeedPerScreen * powerScale * state.bulletSpeedMultiplier
        val bullets = if (state.tripleShotSeconds > 0f) {
            val spreadSpeed = state.screenSize.x * 0.34f
            listOf(
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x - state.player.size.x * 0.12f, y = bulletY), bulletRadius, bulletSpeed, velocityX = -spreadSpeed),
                Bullet(nextBulletId++, state.player.position.copy(y = bulletY), bulletRadius, bulletSpeed),
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x + state.player.size.x * 0.12f, y = bulletY), bulletRadius, bulletSpeed, velocityX = spreadSpeed),
            )
        } else if (state.doubleShotSeconds > 0f) {
            listOf(
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x - state.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
                Bullet(nextBulletId++, state.player.position.copy(x = state.player.position.x + state.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
            )
        } else {
            listOf(Bullet(nextBulletId++, state.player.position.copy(y = bulletY), bulletRadius, bulletSpeed))
        }
        val cooldown = GameConfig.FireCooldownSeconds / state.bulletSpeedMultiplier.coerceAtLeast(1f)
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
        val movedBosses = state.bosses.map { it.update(deltaSeconds) }
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
        val nextSpeedPowerSeconds = (state.rapidFireSeconds - deltaSeconds).coerceAtLeast(0f)

        val timedState = state.copy(
            bullets = movedBullets,
            enemies = movedEnemies,
            bosses = movedBosses,
            powerUps = movedPowerUps,
            enemyBullets = movedEnemyBullets,
            explosions = activeExplosions,
            combo = nextCombo,
            comboTimerSeconds = nextComboTimer,
            doubleShotSeconds = (state.doubleShotSeconds - deltaSeconds).coerceAtLeast(0f),
            tripleShotSeconds = (state.tripleShotSeconds - deltaSeconds).coerceAtLeast(0f),
            rapidFireSeconds = nextSpeedPowerSeconds,
            bulletSpeedMultiplier = if (nextSpeedPowerSeconds <= 0f) 1f else state.bulletSpeedMultiplier,
            fireCooldownSeconds = (state.fireCooldownSeconds - deltaSeconds).coerceAtLeast(0f),
            enemyFireTimerSeconds = (state.enemyFireTimerSeconds - deltaSeconds).coerceAtLeast(0f),
            bossFireTimerSeconds = (state.bossFireTimerSeconds - deltaSeconds).coerceAtLeast(0f),
            bossPowerUpTimerSeconds = if (state.bosses.isNotEmpty()) {
                (state.bossPowerUpTimerSeconds - deltaSeconds).coerceAtLeast(0f)
            } else {
                0f
            },
            playerHitFlashSeconds = (state.playerHitFlashSeconds - deltaSeconds).coerceAtLeast(0f),
        )

        val afterAutoFire = if (timedState.isTouching) {
            fireFromState(timedState)
        } else {
            timedState
        }
        val afterBulletHits = resolveBossCollisions(
            resolveBulletEnemyCollisions(afterAutoFire),
        )
        val afterPowerUps = resolvePowerUpCollection(afterBulletHits)
        val afterEnemyFire = updateEnemyFire(afterPowerUps)
        val afterBossEscape = resolveBossEscape(afterEnemyFire)
        val afterPlayerDamage = resolvePlayerDamage(afterBossEscape)

        val aliveState = if (afterPlayerDamage.lives <= 0) {
            afterPlayerDamage.copy(
                isGameOver = true,
                isTouching = false,
                doubleShotSeconds = 0f,
                tripleShotSeconds = 0f,
                rapidFireSeconds = 0f,
                bulletSpeedMultiplier = 1f,
                shieldCharges = 0,
            )
        } else {
            afterPlayerDamage.copy(isGameOver = false)
        }

        state = if (aliveState.isGameOver) {
            aliveState
        } else {
            val spawnedState = stageManager.update(
                state = aliveState,
                deltaSeconds = deltaSeconds,
                nextEnemyId = { nextEnemyId++ },
                nextGroupId = { nextGroupId++ },
                nextBossId = { nextBossId++ },
            )
            if (aliveState.bosses.isEmpty() && spawnedState.bosses.isNotEmpty()) {
                addBossPowerUps(spawnedState, includeTripleShot = true).copy(
                    bossPowerUpTimerSeconds = GameConfig.BossPowerUpDropIntervalSeconds,
                )
            } else if (spawnedState.bosses.isNotEmpty() && spawnedState.bossPowerUpTimerSeconds <= 0f) {
                addBossPowerUps(spawnedState, includeTripleShot = false).copy(
                    bossPowerUpTimerSeconds = GameConfig.BossPowerUpDropIntervalSeconds,
                )
            } else {
                spawnedState
            }
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
            if (bullet.id in hitBulletIds) return@forEach

            // A player bullet is consumed by exactly one enemy. If a bullet overlaps a tight group,
            // the nearest enemy is selected and the same bullet cannot remove another insect.
            input.enemies
                .filter { enemy -> enemy.id !in hitEnemyIds && Collision.circlesIntersect(bullet, enemy) }
                .minByOrNull { enemy -> distanceSquared(bullet.position, enemy.position) }
                ?.let { enemy ->
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

    private fun distanceSquared(a: Vector2, b: Vector2): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun resolveBossCollisions(input: GameState): GameState {
        if (input.bosses.isEmpty()) return input

        val hitBulletIds = mutableSetOf<Int>()
        val sparks = mutableListOf<Explosion>()
        val defeatedBosses = mutableListOf<BossMosquito>()
        val aliveBosses = mutableListOf<BossMosquito>()

        input.bosses.forEach { boss ->
            val hitBullets = input.bullets.filter {
                it.id !in hitBulletIds && Collision.circlesIntersect(it, boss)
            }
            if (hitBullets.isEmpty()) {
                aliveBosses += boss
            } else {
                hitBulletIds += hitBullets.map { it.id }
                sparks += hitBullets.map {
                    Explosion(nextExplosionId++, it.position, boss.radius * 0.5f)
                }
                val damagedBoss = boss.damage(hitBullets.size)
                if (damagedBoss.hp > 0) {
                    aliveBosses += damagedBoss
                } else {
                    defeatedBosses += boss
                }
            }
        }

        if (hitBulletIds.isEmpty()) return input

        val bossExplosions = defeatedBosses.map {
            Explosion(nextExplosionId++, it.position, it.radius * 3f)
        }
        val clearedBossWave = input.bosses.isNotEmpty() && aliveBosses.isEmpty()
        val defeatedNames = defeatedBosses.joinToString("+") { "${it.enemyKind.name}_KING_DEFEATED" }
        val nextStage = if (clearedBossWave) nextStageAfterBossWave(input.currentStage) else input.currentStage
        val nextDifficultyLevel = if (clearedBossWave && input.currentStage >= 10) {
            input.difficultyLevel + 1
        } else {
            input.difficultyLevel
        }

        return input.copy(
            bullets = input.bullets.filterNot { it.id in hitBulletIds },
            bosses = aliveBosses,
            enemyBullets = if (clearedBossWave) emptyList() else input.enemyBullets,
            explosions = input.explosions + sparks + bossExplosions,
            score = input.score + defeatedBosses.size * GameConfig.BossScoreBonus * ComboSystem.multiplier(input.combo),
            lives = if (clearedBossWave) {
                (input.lives + 1).coerceAtMost(GameConfig.MaxLives)
            } else {
                input.lives
            },
            combo = input.combo + defeatedBosses.sumOf { it.maxHp },
            comboTimerSeconds = if (defeatedBosses.isEmpty()) input.comboTimerSeconds else ComboSystem.refreshedTimer(),
            currentPatternName = if (defeatedBosses.isEmpty()) input.currentPatternName else defeatedNames,
            currentStage = nextStage,
            difficultyLevel = nextDifficultyLevel,
            normalGroupsSinceBoss = if (clearedBossWave) 0 else input.normalGroupsSinceBoss,
        )
    }

    private fun resolveBossEscape(input: GameState): GameState {
        if (input.bosses.isEmpty()) return input
        val escapedBosses = input.bosses.filter { it.escaped }
        if (escapedBosses.isEmpty()) return input

        return input.copy(
            bosses = input.bosses.map { boss ->
                if (boss.escaped) boss.reenterFromTop() else boss
            },
            currentPatternName = escapedBosses.joinToString("+") { "${it.enemyKind.name}_KING_REENTER" },
        )
    }

    private fun resolvePowerUpCollection(input: GameState): GameState {
        val collected = input.powerUps.filter { Collision.circlesIntersect(input.player, it) }
        if (collected.isEmpty()) return input

        var next = input.copy(powerUps = input.powerUps.filterNot { powerUp -> collected.any { it.id == powerUp.id } })
        collected.forEach { powerUp ->
            next = when (powerUp.type) {
                PowerUpType.SPEED_2 -> next.copy(
                    rapidFireSeconds = next.rapidFireSeconds + GameConfig.PowerUpDurationSeconds,
                    bulletSpeedMultiplier = next.bulletSpeedMultiplier.coerceAtLeast(1.25f),
                )
                PowerUpType.SPEED_4 -> next.copy(
                    rapidFireSeconds = next.rapidFireSeconds + GameConfig.PowerUpDurationSeconds,
                    bulletSpeedMultiplier = 1.5f,
                )
                PowerUpType.DOUBLE_SHOT -> next.copy(doubleShotSeconds = persistentPowerUpSeconds)
                PowerUpType.TRIPLE_SHOT -> next.copy(
                    tripleShotSeconds = next.tripleShotSeconds + GameConfig.PowerUpDurationSeconds,
                )
                PowerUpType.SHIELD_1 -> next.copy(shieldCharges = next.shieldCharges.coerceAtLeast(4))
                PowerUpType.SHIELD_2 -> next.copy(shieldCharges = next.shieldCharges.coerceAtLeast(8))
            }
        }
        return next
    }

    private fun activateBomb(input: GameState): GameState {
        val blastExplosions = listOf(
            Explosion(nextExplosionId++, input.player.position, input.player.size.x * 1.8f),
        )
        return input.copy(
            // Keep enemy removal tied to bullets. The bomb is defensive for now so whole waves
            // cannot disappear without visibly being shot.
            enemyBullets = emptyList(),
            explosions = input.explosions + blastExplosions,
        )
    }

    private fun updateEnemyFire(input: GameState): GameState {
        var next = input
        if (next.enemies.isNotEmpty() && next.enemyFireTimerSeconds <= 0f) {
            val shooter = next.enemies.random()
            val interval = if (next.currentPatternName.contains(FormationPattern.SPIRAL_DOWN.name)) {
                GameConfig.SpiralEnemyFireIntervalSeconds
            } else {
                GameConfig.SpiralEnemyFireIntervalSeconds * 1.35f
            }
            next = next.copy(
                enemyBullets = next.enemyBullets + createEnemyBullet(shooter.position, shooter.enemyKind),
                enemyFireTimerSeconds = interval,
            )
        }

        if (next.bosses.isNotEmpty() && next.bossFireTimerSeconds <= 0f) {
            val bossBullets = next.bosses.flatMap { boss ->
                listOf(
                    createEnemyBullet(boss.position.copy(x = boss.position.x - boss.radius * 0.32f, y = boss.position.y + boss.radius), boss.enemyKind),
                    createEnemyBullet(boss.position.copy(x = boss.position.x + boss.radius * 0.32f, y = boss.position.y + boss.radius), boss.enemyKind),
                )
            }
            next = next.copy(
                enemyBullets = next.enemyBullets + bossBullets,
                bossFireTimerSeconds = GameConfig.BossFireIntervalSeconds,
            )
        }
        return next
    }

    private fun createEnemyBullet(position: Vector2, kind: EnemyKind): EnemyBullet {
        val attackScale = enemyAttackScale(kind)
        val radius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.EnemyBulletRadiusRatio * attackScale
        return EnemyBullet(
            id = nextEnemyBulletId++,
            position = position,
            radius = radius,
            speed = state.screenSize.y * GameConfig.EnemyBulletSpeedPerScreen * attackScale,
        )
    }

    private fun maybeCreatePowerUp(enemy: Enemy): PowerUp? {
        if (Random.nextFloat() > GameConfig.PowerUpDropChance) return null
        val radius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.PowerUpRadiusRatio
        return PowerUp(
            id = nextPowerUpId++,
            type = normalPowerUpTypes.random(),
            position = enemy.position,
            radius = radius,
            speed = state.screenSize.y * GameConfig.PowerUpSpeedPerScreen,
        )
    }

    private fun addBossPowerUps(input: GameState, includeTripleShot: Boolean): GameState {
        val minSize = input.screenSize.x.coerceAtMost(input.screenSize.y)
        val radius = minSize * GameConfig.PowerUpRadiusRatio
        val speed = input.screenSize.y * GameConfig.PowerUpSpeedPerScreen
        val y = input.screenSize.y * 0.18f
        val tripleShotPowerUp = if (includeTripleShot) {
            input.bosses.map { boss ->
                PowerUp(
                    id = nextPowerUpId++,
                    type = PowerUpType.TRIPLE_SHOT,
                    position = Vector2(boss.position.x.coerceIn(radius, input.screenSize.x - radius), y),
                    radius = radius,
                    speed = speed,
                )
            }
        } else {
            emptyList()
        }
        val speedPowerUps = listOf(
            PowerUp(
                id = nextPowerUpId++,
                type = PowerUpType.SPEED_2,
                position = Vector2(input.screenSize.x * 0.32f, y),
                radius = radius,
                speed = speed,
            ),
            PowerUp(
                id = nextPowerUpId++,
                type = PowerUpType.SPEED_4,
                position = Vector2(input.screenSize.x * 0.68f, y),
                radius = radius,
                speed = speed,
            ),
        )
        return input.copy(powerUps = input.powerUps + tripleShotPowerUp + speedPowerUps)
    }

    private fun enemyScore(enemy: Enemy): Int {
        val kindBonus = when (enemy.enemyKind) {
            EnemyKind.MOSQUITO -> 0
            EnemyKind.FLY -> 2
            EnemyKind.HONEY_BEE -> 3
            EnemyKind.WASP -> 5
            EnemyKind.WHITE_BUTTERFLY -> 5
            EnemyKind.SWALLOWTAIL_BUTTERFLY -> 7
            EnemyKind.STAG_BEETLE -> 9
        }
        return when (enemy.formationPattern) {
            FormationPattern.LINE_HORIZONTAL -> GameConfig.LineHorizontalScore + kindBonus
            FormationPattern.CIRCLE_LEFT,
            FormationPattern.CIRCLE_RIGHT -> GameConfig.CirclePatternScore + kindBonus
            else -> GameConfig.MosquitoBaseScore + kindBonus
        }
    }

    private fun nextStageAfterBossWave(currentStage: Int): Int {
        return if (currentStage >= 10) 1 else currentStage + 1
    }

    private fun enemyAttackScale(kind: EnemyKind): Float {
        return when (kind) {
            EnemyKind.MOSQUITO -> 0.9f
            EnemyKind.FLY -> 1f
            EnemyKind.HONEY_BEE -> 1.05f
            EnemyKind.WASP -> 1.2f
            EnemyKind.WHITE_BUTTERFLY -> 1.1f
            EnemyKind.SWALLOWTAIL_BUTTERFLY -> 1.25f
            EnemyKind.STAG_BEETLE -> 1.4f
        }
    }

    private fun resolvePlayerDamage(input: GameState): GameState {
        val escapedEnemyIds = input.enemies
            .filter { it.position.y - it.radius > input.screenSize.y }
            .mapTo(mutableSetOf()) { it.id }
        val contactEnemyIds = input.enemies
            .filter { it.id !in escapedEnemyIds && Collision.circlesIntersect(input.player, it) }
            .minByOrNull { distanceSquared(input.player.position, it.position) }
            ?.let { mutableSetOf(it.id) }
            ?: mutableSetOf()
        val enemyBulletIds = input.enemyBullets
            .filter { Collision.circlesIntersect(input.player, it) }
            .mapTo(mutableSetOf()) { it.id }
        val bossHit = input.bosses.any { Collision.circlesIntersect(input.player, it) }

        if (escapedEnemyIds.isEmpty() && contactEnemyIds.isEmpty() && enemyBulletIds.isEmpty() && !bossHit) return input

        val stateAfterMisses = input.copy(
            enemies = input.enemies.filterNot { it.id in escapedEnemyIds || it.id in contactEnemyIds },
            enemyBullets = input.enemyBullets.filterNot { it.id in enemyBulletIds },
            misses = input.misses + escapedEnemyIds.size,
            carryOverEnemies = (input.carryOverEnemies + escapedEnemyIds.size)
                .coerceAtMost(GameConfig.MaxCarryOverEnemies),
            currentGroupHadMiss = input.currentGroupHadMiss || escapedEnemyIds.isNotEmpty() || contactEnemyIds.isNotEmpty(),
        )

        val blockedBulletHits = enemyBulletIds.size.coerceAtMost(input.shieldCharges)
        val nextShieldCharges = (input.shieldCharges - blockedBulletHits).coerceAtLeast(0)
        val unblockedBulletHits = enemyBulletIds.size - blockedBulletHits
        val damageEvents = contactEnemyIds.size + unblockedBulletHits + if (bossHit) 1 else 0
        if (damageEvents <= 0 || input.playerHitFlashSeconds > 0f) {
            return stateAfterMisses.copy(shieldCharges = nextShieldCharges)
        }

        val afterBonusPenalty = if (unblockedBulletHits > 0 && input.shieldCharges <= 0) {
            stateAfterMisses.copy(
                doubleShotSeconds = 0f,
                tripleShotSeconds = 0f,
                rapidFireSeconds = 0f,
                bulletSpeedMultiplier = 1f,
                shieldCharges = 0,
            )
        } else {
            stateAfterMisses.copy(shieldCharges = nextShieldCharges)
        }

        return applyPlayerHit(afterBonusPenalty)
    }

    private fun applyPlayerHit(input: GameState): GameState {
        val nextLives = (input.lives - 1).coerceAtLeast(0)
        return input.copy(
            lives = nextLives,
            hits = input.hits + 1,
            score = (input.score - GameConfig.PLAYER_HIT_SCORE_PENALTY).coerceAtLeast(0),
            shieldCharges = if (nextLives > 0) {
                input.shieldCharges.coerceAtLeast(GameConfig.RespawnShieldCharges)
            } else {
                0
            },
            combo = 0,
            comboTimerSeconds = 0f,
            playerHitFlashSeconds = GameConfig.PlayerHitFlashSeconds,
            isGameOver = nextLives <= 0,
        )
    }

    private fun fireFromState(input: GameState): GameState {
        if (input.fireCooldownSeconds > 0f || input.screenSize == Vector2.Zero) return input
        if (input.bullets.size >= GameConfig.MaxPlayerBullets) return input

        val powerScale = playerBulletPowerScale(input)
        val bulletRadius = input.screenSize.x.coerceAtMost(input.screenSize.y) * GameConfig.BulletRadiusRatio * powerScale
        val bulletY = input.player.position.y - input.player.size.y / 2f
        val bulletSpeed = input.screenSize.y * GameConfig.BulletSpeedPerScreen * powerScale * input.bulletSpeedMultiplier
        val newBullets = if (input.tripleShotSeconds > 0f) {
            val spreadSpeed = input.screenSize.x * 0.34f
            listOf(
                Bullet(nextBulletId++, input.player.position.copy(x = input.player.position.x - input.player.size.x * 0.12f, y = bulletY), bulletRadius, bulletSpeed, velocityX = -spreadSpeed),
                Bullet(nextBulletId++, input.player.position.copy(y = bulletY), bulletRadius, bulletSpeed),
                Bullet(nextBulletId++, input.player.position.copy(x = input.player.position.x + input.player.size.x * 0.12f, y = bulletY), bulletRadius, bulletSpeed, velocityX = spreadSpeed),
            )
        } else if (input.doubleShotSeconds > 0f) {
            listOf(
                Bullet(nextBulletId++, input.player.position.copy(x = input.player.position.x - input.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
                Bullet(nextBulletId++, input.player.position.copy(x = input.player.position.x + input.player.size.x * 0.22f, y = bulletY), bulletRadius, bulletSpeed),
            )
        } else {
            listOf(Bullet(nextBulletId++, input.player.position.copy(y = bulletY), bulletRadius, bulletSpeed))
        }
        val cooldown = GameConfig.FireCooldownSeconds / input.bulletSpeedMultiplier.coerceAtLeast(1f)
        return input.copy(
            bullets = (input.bullets + newBullets).takeLast(GameConfig.MaxPlayerBullets),
            fireCooldownSeconds = cooldown,
        )
    }

    private fun playerSizeFor(screenSize: Vector2): Vector2 {
        return Vector2(
            x = screenSize.x * GameConfig.PlayerWidthRatio,
            y = screenSize.y * GameConfig.PlayerHeightRatio,
        )
    }

    private fun playerMinYFor(screenSize: Vector2): Float {
        return screenSize.y * GameConfig.PlayerMinYRatio
    }

    private fun playerBulletPowerScale(input: GameState): Float {
        return 1f + (input.score / 500).coerceAtMost(4) * 0.08f
    }

}
