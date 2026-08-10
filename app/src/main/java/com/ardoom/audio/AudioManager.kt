package com.ardoom.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * Sound manager for DOOM sound effects:
 * - Weapon firing
 * - Enemy growls/attacks
 * - Player damage grunts
 * - Pickup sounds
 * - Background ambience
 *
 * Sound files should be placed in app/src/main/res/raw/
 * Naming convention: sfx_pistol.wav, sfx_shotgun.wav, sfx_imp_growl.wav, etc.
 */
class AudioManager(private val context: Context) {

    private var soundPool: SoundPool
    private val soundMap = HashMap<String, Int>()
    private var loaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                Log.i(TAG, "Sound loaded: $sampleId")
            }
        }
    }

    fun loadSounds() {
        // Load DOOM sound effects from res/raw/
        val soundFiles = mapOf(
            "pistol" to "sfx_pistol",
            "shotgun" to "sfx_shotgun",
            "chaingun" to "sfx_chaingun",
            "imp_growl" to "sfx_imp_growl",
            "imp_attack" to "sfx_imp_attack",
            "soldier_alert" to "sfx_soldier_alert",
            "demon_growl" to "sfx_demon_growl",
            "player_hurt" to "sfx_player_hurt",
            "player_die" to "sfx_player_die",
            "enemy_die" to "sfx_enemy_die",
            "pickup" to "sfx_pickup",
            "level_up" to "sfx_level_up"
        )

        for ((name, resName) in soundFiles) {
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) {
                soundMap[name] = soundPool.load(context, resId, 1)
            } else {
                Log.w(TAG, "Sound file not found: $resName")
            }
        }

        loaded = true
    }

    fun playSound(name: String, volume: Float = 1.0f, rate: Float = 1.0f) {
        if (!loaded) return
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, rate)
    }

    fun playWeaponSound(weapon: com.ardoom.game.Weapon) {
        playSound(weapon.name.lowercase())
    }

    fun playEnemySound(enemyType: com.ardoom.game.EnemyType, isAttack: Boolean = false) {
        val soundName = if (isAttack) {
            "${enemyType.name.lowercase()}_attack"
        } else {
            "${enemyType.name.lowercase()}_growl"
        }
        playSound(soundName)
    }

    fun release() {
        soundPool.release()
        loaded = false
    }

    companion object {
        private const val TAG = "AudioManager"
    }
}
