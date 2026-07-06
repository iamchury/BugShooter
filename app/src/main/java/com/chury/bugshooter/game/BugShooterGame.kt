package com.chury.bugshooter.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chury.bugshooter.engine.Collision
import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2

class BugShooterGame {
    var state by mutableStateOf(GameState())
        private set

    private val stageManager = StageManager()
    private var nextBulletId = 1
    private var nextEnemyId = 1
    private var nextGroupId = 1
    private var nextExplosionId = 1

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

        val bulletRadius = state.screenSize.x.coerceAtMost(state.screenSize.y) * GameConfig.BulletRadiusRatio
        val bullet = Bullet(
            id = nextBulletId++,
            position = state.player.position.copy(y = state.player.position.y - state.player.size.y / 2f),
            radius = bulletRadius,
            speed = state.screenSize.y * GameConfig.BulletSpeedPerScreen,
        )
        state = state.copy(
            bullets = state.bullets + bullet,
            fireCooldownSeconds = GameConfig.FireCooldownSeconds,
        )
    }

    fun update(deltaSeconds: Float) {
        if (state.screenSize == Vector2.Zero || state.isGameOver) return

        val movedBullets = state.bullets
            .map { it.update(deltaSeconds) }
            .filter { it.position.y + it.radius >= 0f }
        val movedEnemies = state.enemies.map { it.update(deltaSeconds) }
        val activeExplosions = state.explosions
            .map { it.update(deltaSeconds) }
            .filterNot { it.isFinished }

        val afterCollision = resolveBulletEnemyCollisions(
            state.copy(
                bullets = movedBullets,
                enemies = movedEnemies,
                explosions = activeExplosions,
                fireCooldownSeconds = (state.fireCooldownSeconds - deltaSeconds).coerceAtLeast(0f),
                playerHitFlashSeconds = (state.playerHitFlashSeconds - deltaSeconds).coerceAtLeast(0f),
            ),
        )

        val afterPlayerDamage = resolvePlayerDamage(afterCollision)

        val aliveState = afterPlayerDamage.copy(isGameOver = afterPlayerDamage.lives <= 0)

        state = if (aliveState.isGameOver) {
            aliveState
        } else {
            stageManager.update(
                state = aliveState,
                deltaSeconds = deltaSeconds,
                nextEnemyId = { nextEnemyId++ },
                nextGroupId = { nextGroupId++ },
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
            }
        }

        return input.copy(
            bullets = input.bullets.filterNot { it.id in hitBulletIds },
            enemies = input.enemies.filterNot { it.id in hitEnemyIds },
            explosions = input.explosions + newExplosions,
            score = input.score + hitEnemyIds.size,
        )
    }

    private fun resolvePlayerDamage(input: GameState): GameState {
        val escapedEnemyIds = input.enemies
            .filter { it.position.y - it.radius > input.screenSize.y }
            .mapTo(mutableSetOf()) { it.id }
        val contactEnemyIds = input.enemies
            .filter { it.id !in escapedEnemyIds && Collision.circlesIntersect(input.player, it) }
            .mapTo(mutableSetOf()) { it.id }
        val damagingEnemyIds = escapedEnemyIds + contactEnemyIds

        if (damagingEnemyIds.isEmpty()) return input

        val enemiesAfterDamage = input.enemies.filterNot { it.id in damagingEnemyIds }
        val stateAfterMisses = input.copy(
            enemies = enemiesAfterDamage,
            misses = input.misses + escapedEnemyIds.size,
        )

        if (contactEnemyIds.isEmpty()) {
            return stateAfterMisses
        }

        if (input.playerHitFlashSeconds > 0f) {
            return stateAfterMisses
        }

        val lives = (stateAfterMisses.lives - 1).coerceAtLeast(0)
        return stateAfterMisses.copy(
            lives = lives,
            hits = stateAfterMisses.hits + 1,
            score = (stateAfterMisses.score - 1).coerceAtLeast(0),
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
