package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.ardoom.ar.ARCameraManager
import com.ardoom.game.EnemyState
import com.ardoom.game.GameEngine
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * The main OpenGL ES 3.0 renderer for AR DOOM.
 *
 * Rendering pipeline:
 *   1. ARCore camera background (the real-world feed from the S25's camera)
 *   2. DOOM enemies rendered as billboarded sprites in AR world space
 *   3. HUD overlay (health, ammo, score) via separate view
 *   4. Muzzle flash / effects on fire
 *
 * Enemies are rendered as billboarded quads that always face the camera,
 * textured with DOOM-style sprite art. This gives the classic DOOM look
 * but in real space.
 */
class DoomRenderer(
    private val context: Context,
    private val cameraManager: ARCameraManager,
    private val gameEngine: GameEngine
) : GLSurfaceView.Renderer {

    private lateinit var backgroundShader: BackgroundShader
    private lateinit var spriteShader: SpriteShader
    private lateinit var effectShader: EffectShader

    private var viewportWidth: Int = 1920
    private var viewportHeight: Int = 1080
    private var lastFrameTime: Long = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        backgroundShader = BackgroundShader(context)
        spriteShader = SpriteShader(context)
        effectShader = EffectShader(context)

        // Load DOOM sprite textures
        SpriteTextureManager.init(context)

        lastFrameTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        cameraManager.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val deltaTime = (now - lastFrameTime) / 1_000_000_000f
        lastFrameTime = now

        // Clear
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Acquire AR frame
        val frame = cameraManager.acquireFrame()
        if (frame == null) {
            return  // AR not ready yet
        }

        // 1. Draw AR camera background (real-world feed)
        backgroundShader.draw(frame)

        // 2. Only render game elements if we're tracking
        if (cameraManager.isTracking(frame)) {
            val camera = cameraManager.getCameraPose(frame)

            // Update game logic
            gameEngine.update(deltaTime, camera, cameraManager, frame)

            // 3. Render enemies as billboards in AR space
            renderEnemies(camera)

            // 4. Render effects (muzzle flash, projectiles)
            renderEffects(camera)
        }

        frame.close()
    }

    /**
     * Render all active enemies as billboarded sprites facing the camera.
     */
    private fun renderEnemies(camera: Camera) {
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)

        for (enemy in gameEngine.getEnemies()) {
            if (enemy.state == EnemyState.DEAD) continue

            val modelMatrix = FloatArray(16)
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)

            // Position enemy at its AR world coordinates
            android.opengl.Matrix.translateM(modelMatrix, 0, enemy.position[0], enemy.position[1], enemy.position[2])

            // Billboard: rotate to face the camera
            billboardMatrix(modelMatrix, camera)

            // Scale based on enemy type (demons are bigger)
            val scale = when (enemy.type) {
                com.ardoom.game.EnemyType.IMP -> 0.5f
                com.ardoom.game.EnemyType.SOLDIER -> 0.45f
                com.ardoom.game.EnemyType.DEMON -> 0.7f
                com.ardoom.game.EnemyType.CACODEMON -> 0.6f
                com.ardoom.game.EnemyType.BARON -> 0.8f
            }
            android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

            // Tint based on enemy state (hurt = red flash, dying = fade)
            val alpha = when (enemy.state) {
                EnemyState.HURT -> 1.0f
                EnemyState.DYING -> 0.5f
                else -> 1.0f
            }

            val texture = SpriteTextureManager.getTexture(enemy.type, enemy.state)
            spriteShader.draw(modelMatrix, viewMatrix, projectionMatrix, texture, alpha)
        }
    }

    /**
     * Apply a billboard rotation so the sprite quad always faces the camera.
     */
    private fun billboardMatrix(modelMatrix: FloatArray, camera: Camera) {
        val cameraPos = FloatArray(3)
        camera.displayOrientedPose.getTranslation(cameraPos, 0)

        // Get enemy position from model matrix translation
        val enemyPos = floatArrayOf(modelMatrix[12], modelMatrix[13], modelMatrix[14])

        // Calculate angle to face camera (Y-axis rotation for upright billboards)
        val dx = cameraPos[0] - enemyPos[0]
        val dz = cameraPos[2] - enemyPos[2]
        val angle = Math.atan2(dx.toDouble(), dz.toDouble()).toFloat()

        android.opengl.Matrix.rotateM(modelMatrix, 0, Math.toDegrees(angle.toDouble()).toFloat(), 0f, 1f, 0f)
    }

    private var muzzleFlashTimer: Float = 0f

    fun triggerMuzzleFlash() {
        muzzleFlashTimer = 0.1f  // 100ms flash
    }

    private fun renderEffects(camera: Camera) {
        if (muzzleFlashTimer > 0) {
            // Draw a bright quad at screen center (simple muzzle flash)
            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            camera.getViewMatrix(viewMatrix, 0)

            val modelMatrix = FloatArray(16)
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)

            // Place flash 0.5m in front of camera
            val cameraPos = FloatArray(3)
            camera.displayOrientedPose.getTranslation(cameraPos, 0)
            android.opengl.Matrix.translateM(modelMatrix, 0, cameraPos[0], cameraPos[1], cameraPos[2] - 0.5f)

            val flashScale = muzzleFlashTimer * 5f
            android.opengl.Matrix.scaleM(modelMatrix, 0, flashScale, flashScale, flashScale)

            effectShader.drawMuzzleFlash(modelMatrix, viewMatrix, projectionMatrix)
            muzzleFlashTimer -= 0.016f  // approx one frame at 60fps
        }
    }
}
