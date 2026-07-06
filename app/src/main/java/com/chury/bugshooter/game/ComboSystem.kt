package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig

object ComboSystem {
    fun multiplier(combo: Int): Int {
        return when {
            combo >= 50 -> 5
            combo >= 25 -> 3
            combo >= 10 -> 2
            else -> 1
        }
    }

    fun refreshedTimer(): Float = GameConfig.ComboTimeoutSeconds
}
