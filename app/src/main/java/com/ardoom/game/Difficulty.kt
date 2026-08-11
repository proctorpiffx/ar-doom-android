package com.ardoom.game

/**
 * Difficulty settings affecting enemy stats, spawn rates, and score multipliers.
 */
enum class Difficulty(
    val displayName: String,
    val hpMultiplier: Float,
    val speedMultiplier: Float,
    val damageMultiplier: Float,
    val spawnRateMultiplier: Float,
    val scoreMultiplier: Float
) {
    EASY("EASY", hpMultiplier = 0.75f, speedMultiplier = 0.8f, damageMultiplier = 0.75f, spawnRateMultiplier = 0.8f, scoreMultiplier = 0.75f),
    MEDIUM("MEDIUM", hpMultiplier = 1.0f, speedMultiplier = 1.0f, damageMultiplier = 1.0f, spawnRateMultiplier = 1.0f, scoreMultiplier = 1.0f),
    HARD("HARD", hpMultiplier = 1.5f, speedMultiplier = 1.25f, damageMultiplier = 1.5f, spawnRateMultiplier = 1.25f, scoreMultiplier = 1.5f),
    NIGHTMARE("NIGHTMARE", hpMultiplier = 2.0f, speedMultiplier = 1.5f, damageMultiplier = 2.0f, spawnRateMultiplier = 1.5f, scoreMultiplier = 2.0f)
}
