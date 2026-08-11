package com.ardoom

import android.content.Context

enum class Difficulty(
    val enemyHealthMultiplier: Float,
    val enemySpeedMultiplier: Float,
    val enemyDamageMultiplier: Float,
    val spawnRateMultiplier: Float,
    val displayName: String
) {
    EASY(0.7f, 0.8f, 0.7f, 0.8f, "EASY"),
    NORMAL(1.0f, 1.0f, 1.0f, 1.0f, "NORMAL"),
    HARD(1.4f, 1.2f, 1.4f, 1.3f, "HARD"),
    NIGHTMARE(2.0f, 1.5f, 2.0f, 1.8f, "NIGHTMARE");

    companion object {
        fun fromName(name: String?): Difficulty {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: NORMAL
        }
    }
}

object GameSettings {
    private const val PREFS_NAME = "ardoom_settings"

    private const val KEY_MASTER_VOLUME = "master_volume"
    private const val KEY_SFX_VOLUME = "sfx_volume"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_DIFFICULTY = "difficulty"
    private const val KEY_CAMERA_SENSITIVITY = "camera_sensitivity"
    private const val KEY_SHOW_FPS = "show_fps"
    private const val KEY_HIGH_SCORE = "high_score"

    var masterVolume: Float = 1.0f
    var sfxVolume: Float = 1.0f
    var hapticsEnabled: Boolean = true
    var difficulty: Difficulty = Difficulty.NORMAL
    var cameraSensitivity: Float = 1.0f
    var showFPS: Boolean = false
    var highScore: Int = 0

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        masterVolume = prefs.getFloat(KEY_MASTER_VOLUME, 1.0f)
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, 1.0f)
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        val diffString = prefs.getString(KEY_DIFFICULTY, Difficulty.NORMAL.name)
        difficulty = Difficulty.fromName(diffString)
        cameraSensitivity = prefs.getFloat(KEY_CAMERA_SENSITIVITY, 1.0f)
        showFPS = prefs.getBoolean(KEY_SHOW_FPS, false)
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_MASTER_VOLUME, masterVolume)
            putFloat(KEY_SFX_VOLUME, sfxVolume)
            putBoolean(KEY_HAPTICS_ENABLED, hapticsEnabled)
            putString(KEY_DIFFICULTY, difficulty.name)
            putFloat(KEY_CAMERA_SENSITIVITY, cameraSensitivity)
            putBoolean(KEY_SHOW_FPS, showFPS)
            putInt(KEY_HIGH_SCORE, highScore)
            apply()
        }
    }

    fun updateHighScore(context: Context, score: Int): Boolean {
        if (score > highScore) {
            highScore = score
            save(context)
            return true
        }
        return false
    }
}
