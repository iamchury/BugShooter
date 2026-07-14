package com.chury.bugshooter.game

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Small generated-audio bridge for the first sound pass.
 * Real BGM/SFX assets can later replace these calls without touching gameplay rules.
 */
object SoundController {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
    private val deathToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val soundHandler = Handler(Looper.getMainLooper())

    fun playBackgroundBeat(step: Int) {
        val tone = when (step % 8) {
            0, 4 -> ToneGenerator.TONE_PROP_BEEP2
            2, 6 -> ToneGenerator.TONE_CDMA_CONFIRM
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        val durationMs = if (step % 4 == 0) 115 else 70
        toneGenerator.startTone(tone, durationMs)
    }

    fun stopBackground() {
        toneGenerator.stopTone()
    }

    fun playEnemyHit() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 45)
    }

    fun playPlayerHit() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 120)
    }

    fun playPlayerRevive() {
        toneGenerator.stopTone()
        playTone(ToneGenerator.TONE_PROP_ACK, 0L, 120)
        playTone(ToneGenerator.TONE_CDMA_CONFIRM, 130L, 160)
        playTone(ToneGenerator.TONE_PROP_BEEP2, 300L, 220)
    }

    fun playPlayerDeath() {
        stopBackground()
        toneGenerator.stopTone()
        deathToneGenerator.stopTone()
        soundHandler.removeCallbacksAndMessages(null)
        playDeathTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 0L, 260)
        playDeathTone(ToneGenerator.TONE_PROP_NACK, 230L, 420)
        playDeathTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE, 600L, 520)
        playDeathTone(ToneGenerator.TONE_PROP_NACK, 1050L, 650)
    }

    private fun playDeathTone(tone: Int, delayMs: Long, durationMs: Int) {
        soundHandler.postDelayed({
            deathToneGenerator.startTone(tone, durationMs)
        }, delayMs)
    }

    private fun playTone(tone: Int, delayMs: Long, durationMs: Int) {
        soundHandler.postDelayed({
            toneGenerator.startTone(tone, durationMs)
        }, delayMs)
    }
}
