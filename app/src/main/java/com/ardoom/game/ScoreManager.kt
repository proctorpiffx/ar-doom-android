package com.ardoom.game

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages score persistence using Android SharedPreferences.
 */
object ScoreManager {

    private const val PREFS_NAME = "ar_doom_scores"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_HIGH_WAVE = "high_wave"
    private const val KEY_TOTAL_KILLS = "total_kills"
    private const val KEY_GAMES_PLAYED = "games_played"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHighScore(context: Context): Int {
        return getPrefs(context).getInt(KEY_HIGH_SCORE, 0)
    }

    fun getHighWave(context: Context): Int {
        return getPrefs(context).getInt(KEY_HIGH_WAVE, 1)
    }

    fun getTotalKills(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_KILLS, 0)
    }

    fun saveGameStats(context: Context, stats: GameStats): Boolean {
        val prefs = getPrefs(context)
        val currentHighScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        val currentHighWave = prefs.getInt(KEY_HIGH_WAVE, 1)
        val currentKills = prefs.getInt(KEY_TOTAL_KILLS, 0)
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)

        val isNewHighScore = stats.score > currentHighScore

        prefs.edit().apply {
            if (isNewHighScore) {
                putInt(KEY_HIGH_SCORE, stats.score)
            }
            if (stats.wave > currentHighWave) {
                putInt(KEY_HIGH_WAVE, stats.wave)
            }
            putInt(KEY_TOTAL_KILLS, currentKills + stats.kills)
            putInt(KEY_GAMES_PLAYED, gamesPlayed + 1)
            apply()
        }

        return isNewHighScore
    }
}
