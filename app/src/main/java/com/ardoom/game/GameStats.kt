package com.ardoom.game

/**
 * Data class capturing end-of-game or current game session statistics.
 */
data class GameStats(
    val score: Int = 0,
    val wave: Int = 1,
    val kills: Int = 0,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val timePlayed: Float = 0f, // in seconds
    val difficulty: String = "MEDIUM"
) {
    val accuracy: Float
        get() = if (shotsFired > 0) (shotsHit.toFloat() / shotsFired.toFloat()).coerceIn(0f, 1f) else 0f

    val rank: String
        get() = when {
            score < 500 -> "Rookie"
            score < 1500 -> "Marine"
            score < 3500 -> "Veteran"
            score < 7000 -> "Demon Slayer"
            else -> "Legend"
        }
}
