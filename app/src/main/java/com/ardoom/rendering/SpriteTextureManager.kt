package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import com.ardoom.game.EnemyState
import com.ardoom.game.EnemyType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Loads and manages DOOM-style sprite textures for enemies.
 * Each enemy type has sprites for different states (idle, walking, hurt, dying).
 *
 * In production, these would be loaded from sprite sheet PNGs in assets/.
 * For now, we generate simple colored quads as placeholders that can be
 * swapped with real DOOM sprite art.
 */
object SpriteTextureManager {

    private var initialized = false
    private val textureMap = HashMap<String, Int>()

    fun init(context: Context) {
        if (initialized) return

        // Generate placeholder textures for each enemy type + state
        for (type in EnemyType.values()) {
            for (state in listOf(EnemyState.IDLE, EnemyState.HURT, EnemyState.DYING)) {
                val textureId = generatePlaceholderTexture(type, state)
                textureMap[key(type, state)] = textureId
            }
        }

        initialized = true
    }

    fun getTexture(type: EnemyType, state: EnemyState): Int {
        // Fall back to IDLE if we don't have a texture for this state
        return textureMap[key(type, state)] ?: textureMap[key(type, EnemyState.IDLE)] ?: 0
    }

    private fun key(type: EnemyType, state: EnemyState): String = "${type.name}_${state.name}"

    /**
     * Generate a simple colored 64x64 texture as a placeholder.
     * Each enemy type gets a distinct color:
     *   IMP = orange/red, SOLDIER = brown, DEMON = pink, etc.
     *
     * Replace these with actual DOOM sprite PNGs loaded from assets/sprites/
     */
    private fun generatePlaceholderTexture(type: EnemyType, state: EnemyState): Int {
        val size = 64
        val pixels = IntBuffer.allocate(size * size)

        val (r, g, b) = when (type) {
            EnemyType.IMP -> Triple(220, 80, 30)     // orange-red
            EnemyType.SOLDIER -> Triple(120, 80, 50)  // brown
            EnemyType.DEMON -> Triple(200, 140, 150)  // pink
            EnemyType.CACODEMON -> Triple(200, 30, 30) // red
            EnemyType.BARON -> Triple(80, 200, 80)    // green
        }

        // Modify color based on state
        val (fr, fg, fb) = when (state) {
            EnemyState.HURT -> Triple(255, 255, 255)  // white flash
            EnemyState.DYING -> Triple(r / 2, g / 2, b / 2)  // darker
            else -> Triple(r, g, b)
        }

        for (i in 0 until size * size) {
            // Create a simple silhouette shape
            val x = i % size
            val y = i / size
            val cx = size / 2
            val cy = size / 2
            val dist = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble())

            if (dist < size / 3) {
                // Body
                val alpha = if (dist < size / 3 - 2) 255 else 128
                pixels.put((alpha shl 24) or (fr shl 16) or (fg shl 8) or fb)
            } else {
                pixels.put(0)  // transparent
            }
        }
        pixels.position(0)

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            size, size, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels
        )

        return textures[0]
    }
}
