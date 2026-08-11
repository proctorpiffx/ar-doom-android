package com.ardoom.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager as SystemAudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log

/**
 * Sound manager for DOOM sound effects and background music:
 * - Weapon firing
 * - Enemy growls/attacks
 * - Player damage grunts
 * - Pickup sounds
 * - Background ambient drone (synthesized)
 * - Wave completion & player death
 * - Volume control (master & sfx)
 *
 * Sound files are loaded from app/src/main/res/raw/
 */
class AudioManager(private val context: Context) {

    private var soundPool: SoundPool
    private val soundMap = HashMap<String, Int>()
    private var loaded = false

    // Volume settings
    private var masterVolume: Float = 1.0f
    private var sfxVolume: Float = 1.0f

    // Background ambient music
    private var audioTrack: AudioTrack? = null
    @Volatile private var isDronePlaying = false
    private var droneThread: Thread? = null

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
        val finalVolume = (volume * sfxVolume * masterVolume).coerceIn(0f, 1f)
        soundPool.play(soundId, finalVolume, finalVolume, 1, 0, rate)
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

    fun playPickupSound() {
        playSound("pickup")
    }

    fun playWaveCompleteSound() {
        playSound("level_up")
    }

    fun playPlayerDeathSound() {
        playSound("player_die")
    }

    fun playMenuClickSound() {
        // Fallback to high pitch pickup sound or synthesized short tone
        if (soundMap.containsKey("pickup")) {
            playSound("pickup", volume = 0.5f, rate = 2.0f)
        } else {
            playSynthesizedTone(frequency = 800.0, durationMs = 40)
        }
    }

    fun setMasterVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
    }

    fun setSfxVolume(vol: Float) {
        sfxVolume = vol.coerceIn(0f, 1f)
    }

    /**
     * Programmatically generates and loops a low-frequency dark ambient drone
     * (55Hz sub-bass with 82.4Hz fifth overlay) for DOOM ambient background music.
     */
    fun startBackgroundMusic() {
        if (isDronePlaying) return
        isDronePlaying = true

        droneThread = Thread {
            val sampleRate = 22050
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRate / 5)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AudioTrack for background music", e)
                isDronePlaying = false
                return@Thread
            }

            audioTrack = track
            try {
                track.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play AudioTrack", e)
                isDronePlaying = false
                return@Thread
            }

            val pcm = ShortArray(bufferSize / 2)
            var phase1 = 0.0
            var phase2 = 0.0
            val freq1 = 55.0   // A1 sub-bass drone
            val freq2 = 82.41  // E2 fifth overlay

            while (isDronePlaying) {
                val ambientVol = (masterVolume * 0.25f).coerceIn(0f, 1f)
                for (i in pcm.indices) {
                    val sample1 = Math.sin(phase1)
                    val sample2 = Math.sin(phase2) * 0.5
                    val combined = (sample1 + sample2) * 0.5
                    pcm[i] = (combined * 32767 * ambientVol).toInt().coerceIn(-32768, 32767).toShort()

                    phase1 += 2.0 * Math.PI * freq1 / sampleRate
                    phase2 += 2.0 * Math.PI * freq2 / sampleRate
                    if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                    if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                }
                track.write(pcm, 0, pcm.size)
            }

            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioTrack", e)
            }
        }.apply {
            name = "DoomAmbientDrone"
            start()
        }
    }

    fun stopBackgroundMusic() {
        isDronePlaying = false
        try {
            droneThread?.join(300)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining drone thread", e)
        }
        audioTrack = null
        droneThread = null
    }

    fun pauseBackgroundMusic() {
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing background music", e)
        }
    }

    fun resumeBackgroundMusic() {
        try {
            if (isDronePlaying && audioTrack != null) {
                audioTrack?.play()
            } else {
                startBackgroundMusic()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming background music", e)
        }
    }

    private fun playSynthesizedTone(frequency: Double, durationMs: Long) {
        Thread {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)
                var phase = 0.0

                val vol = (0.5f * sfxVolume * masterVolume).coerceIn(0f, 1f)
                for (i in 0 until numSamples) {
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    samples[i] = (Math.sin(phase) * envelope * 32767 * vol).toInt().toShort()
                    phase += 2.0 * Math.PI * frequency / sampleRate
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, numSamples)
                track.play()
                Thread.sleep(durationMs + 50)
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Tone playback error", e)
            }
        }.start()
    }

    fun release() {
        stopBackgroundMusic()
        soundPool.release()
        loaded = false
    }

    companion object {
        private const val TAG = "AudioManager"
    }
}
