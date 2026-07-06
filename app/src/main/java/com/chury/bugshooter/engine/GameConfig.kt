package com.chury.bugshooter.engine

object GameConfig {
    const val InitialLives = 3
    const val PlayerWidthRatio = 0.14f
    const val PlayerHeightRatio = 0.07f
    const val PlayerCenterYRatio = 0.86f
    const val PlayerMinYRatio = 0.12f
    const val PLAYER_HIT_SCORE_PENALTY = 100
    const val BulletRadiusRatio = 0.012f
    const val BulletSpeedPerScreen = 0.9f
    const val MaxPlayerBullets = 36
    const val MosquitoRadiusRatio = 0.045f
    const val MosquitoSpeedPerScreen = 0.24f
    const val FireCooldownSeconds = 0.28f
    const val RapidFireCooldownSeconds = 0.11f
    const val ExplosionLifetimeSeconds = 0.3f
    const val PlayerHitFlashSeconds = 0.7f
    const val ComboTimeoutSeconds = 2.2f
    const val PowerUpDropChance = 0.1f
    const val PowerUpDurationSeconds = 8f
    const val PowerUpRadiusRatio = 0.022f
    const val PowerUpSpeedPerScreen = 0.16f
    const val EnemyBulletRadiusRatio = 0.012f
    const val EnemyBulletSpeedPerScreen = 0.36f
    const val SpiralEnemyFireIntervalSeconds = 1.1f
    const val BossFireIntervalSeconds = 0.75f
    const val BossHp = 30
    const val BossScoreBonus = 300
    const val BossEveryNormalGroups = 5
    const val MosquitoBaseScore = 10
    const val LineHorizontalScore = 5
    const val CirclePatternScore = 20
    const val MinEnemiesPerGroup = 10
    const val MaxEnemiesPerGroup = 10
    const val NextGroupDelaySeconds = 1f
    const val FormationSpeedMinPerScreen = 0.18f
    const val FormationSpeedMaxPerScreen = 0.3f
    const val FormationSpacingRatio = 0.08f
    const val ZigzagAmplitudeRatio = 0.18f
    const val WaveAmplitudeRatio = 0.045f
    const val CircleRadiusRatio = 0.13f
    const val SpiralRadiusRatio = 0.18f
    const val FormationTiltAmplitude = 0.42f
    const val FormationRowWaveRatio = 0.035f
}
