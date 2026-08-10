package com.ardoom.rendering

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import com.ardoom.game.EnemyState
import com.ardoom.game.EnemyType
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Loads and manages DOOM sprite textures from app/src/main/assets/sprites/.
 *
 * Each enemy type has three variants:
 *   {type}_idle.png  — normal state
 *   {type}_hurt.png  — red-tinted when taking damage
 *   {type}_dying.png — darkened, semi-transparent when dying
 *
 * If asset files are missing, falls back to generated placeholder textures.
 */
object SpriteTextureManager {

    private var initialized = false
    private val textureMap = HashMap<String, Int>()
    private lateinit var context: Context

    fun init(context: Context) {
        if (initialized) return
        this.context = context

        // Try loading real sprite assets first
        for (type in EnemyType.values()) {
            val typeName = type.name.lowercase()
            for (state in listOf(EnemyState.IDLE, EnemyState.HURT, EnemyState.DYING)) {
                val stateName = state.name.lowercase()
                val fileName = "sprites/${typeName}_${stateName}.png"

                try {
                    val asset = context.assets.open(fileName)
                    val bitmap = BitmapFactory.decodeStream(asset)
                    asset.close()

                    if (bitmap != null) {
                        val textureId = loadTextureFromBitmap(bitmap)
                        textureMap[key(type, state)] = textureId
                        android.util.Log.i("SpriteTexMgr", "Loaded sprite: $fileName (${bitmap.width}x${bitmap.height})")
                        bitmap.recycle()
                    } else {
                        // Fallback to placeholder
                        textureMap[key(type, state)] = generatePlaceholderTexture(type, state)
                        android.util.Log.w("SpriteTexMgr", "Failed to decode: $fileName — using placeholder")
                    }
                } catch (e: Exception) {
                    // Asset not found — use generated placeholder
                    textureMap[key(type, state)] = generatePlaceholderTexture(type, state)
                    android.util.Log.w("SpriteTexMgr", "Sprite not found: $fileName — using placeholder")
                }
            }
        }

        initialized = true
    }

    fun getTexture(type: EnemyType, state: EnemyState): Int {
        return textureMap[key(type, state)]
            ?: textureMap[key(type, EnemyState.IDLE)]
            ?: 0
    }

    private fun key(type: EnemyType, state: EnemyState): String = "${type.name}_${state.name}"

    /**
     * Load a Bitmap into an OpenGL ES texture.
     * Uses GL_NEAREST for that crisp pixel-art look.
     */
    private fun loadTextureFromBitmap(bitmap: android.graphics.Bitmap): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])

        // Pixel-art: use nearest-neighbor for crisp edges
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Premultiply alpha for correct blending
        GLES30.glPixelStorei(GLES30.GL_UNPACK_PREMULTIPLY_ALPHA_WEBGL, 1)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, bitmap, 0)

        return textures[0]
    }

    /**
     * Fallback: generate a simple colored texture when asset is missing.
     */
    private fun generatePlaceholderTexture(type: EnemyType, state: EnemyState): Int {
        val size = 64
        val pixels = IntBuffer.allocate(size * size)

        val (r, g, b) = when (type) {
            EnemyType.IMP -> Triple(220, 80, 30)
            EnemyType.SOLDIER -> Triple(120, 80, 50)
            EnemyType.DEMON -> Triple(200, 140, 150)
            EnemyType.CACODEMON -> Triple(200, 30, 30)
            EnemyType.BARON -> Triple(80, 200, 80)
        }

        val (fr, fg, fb) = when (state) {
            EnemyState.HURT -> Triple(255, 255, 255)
            EnemyState.DYING -> Triple(r / 2, g / 2, b / 2)
            else -> Triple(r, g, b)
        }

        for (i in 0 until size * size) {
            val x = i % size
            val y = i / size
            val cx = size / 2
            val cy = size / 2
            val dist = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble())

            if (dist < size / 3) {
                val alpha = if (dist < size / 3 - 2) 255 else 128
                pixels.put((alpha shl 24) or (fr shl 16) or (fg shl 8) or fb)
            } else {
                pixels.put(0)
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
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, size, size, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels)

        return textures[0]
    }
}
