package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.ardoom.ar.ARCameraManager
import com.ardoom.game.EnemyState
import com.ardoom.game.GameEngine
import com.ardoom.game.GameState
import com.google.ar.core.Camera
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class DoomRenderer(
    private val context: Context,
    private val cameraManager: ARCameraManager,
    private val gameEngine: GameEngine
) : GLSurfaceView.Renderer {

    interface RendererCallbacks {
        fun onFire()
        fun onHit()
        fun onPlayerHit()
        fun onWaveStart(wave: Int)
    }

    private var callbacks: RendererCallbacks? = null
    private var lastPlayerHitTime = 0L
    private var lastWave = 1
    private var hudUpdateTimer = 0f

    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var spriteShader: SpriteShader
    private lateinit var effectShader: EffectShader

    private var viewportWidth: Int = 1920
    private var viewportHeight: Int = 1080
    private var lastFrameTime: Long = 0
    private var muzzleFlashTimer: Float = 0f
    private var muzzleFlashScreenX: Float = 0.5f
    private var muzzleFlashScreenY: Float = 0.5f

    fun setCallbacks(
        onFire: () -> Unit,
        onHit: () -> Unit,
        onPlayerHit: () -> Unit,
        onWaveStart: (Int) -> Unit
    ) {
        callbacks = object : RendererCallbacks {
            override fun onFire() = onFire()
            override fun onHit() = onHit()
            override fun onPlayerHit() = onPlayerHit()
            override fun onWaveStart(wave: Int) = onWaveStart(wave)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        backgroundRenderer = BackgroundRenderer()
        spriteShader = SpriteShader(context)
        effectShader = EffectShader(context)
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

        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val frame = cameraManager.acquireFrame()
        if (frame == null) return

        try {
            // Draw AR camera background
            backgroundRenderer.draw(cameraManager.session, frame)

            if (cameraManager.isTracking(frame)) {
                val camera = cameraManager.getCameraPose(frame)

                // Update game logic
                gameEngine.update(deltaTime, camera, cameraManager, frame)

                // Fire callback if player was hit
                val now2 = System.currentTimeMillis()
                if (gameEngine.playerWasHit && now2 - lastPlayerHitTime > 500) {
                    lastPlayerHitTime = now2
                    callbacks?.onPlayerHit()
                }
                gameEngine.playerWasHit = false

                // Wave change callback
                if (gameEngine.waveNumber != lastWave) {
                    lastWave = gameEngine.waveNumber
                    callbacks?.onWaveStart(lastWave)
                }

                // Render enemies
                renderEnemies(camera)

                // Render effects
                renderEffects(camera)

                // Update HUD periodically (every ~0.5s)
                hudUpdateTimer += deltaTime
                if (hudUpdateTimer > 0.25f) {
                    hudUpdateTimer = 0f
                    (context as? com.ardoom.MainActivity)?.updateHUD()
                }

                // Check for enemy deaths
                if (gameEngine.enemyDiedThisFrame) {
                    callbacks?.onHit()
                    gameEngine.enemyDiedThisFrame = false
                }
            }
        } finally {
            frame.close()
        }
    }

    fun fire(screenX: Float, screenY: Float) {
        if (gameEngine.gameState != GameState.PLAYING) return
        if (gameEngine.ammo <= 0) return

        // Get the current frame and fire
        val frame = cameraManager.acquireFrame()
        if (frame != null) {
            gameEngine.fire(screenX, screenY, cameraManager, frame)
            muzzleFlashTimer = 0.12f
            muzzleFlashScreenX = screenX / viewportWidth
            muzzleFlashScreenY = screenY / viewportHeight
            callbacks?.onFire()
            frame.close()
        }
    }

    private fun renderEnemies(camera: Camera) {
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)

        for (enemy in gameEngine.getEnemies()) {
            if (enemy.state == EnemyState.DEAD) continue

            val modelMatrix = FloatArray(16)
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)
            android.opengl.Matrix.translateM(modelMatrix, 0, enemy.position[0], enemy.position[1], enemy.position[2])

            // Billboard rotation
            billboardMatrix(modelMatrix, camera)

            val scale = when (enemy.type) {
                com.ardoom.game.EnemyType.IMP -> 0.5f
                com.ardoom.game.EnemyType.SOLDIER -> 0.45f
                com.ardoom.game.EnemyType.DEMON -> 0.7f
                com.ardoom.game.EnemyType.CACODEMON -> 0.6f
                com.ardoom.game.EnemyType.BARON -> 0.8f
            }
            android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

            val alpha = when (enemy.state) {
                EnemyState.HURT -> 1.0f
                EnemyState.DYING -> 0.5f
                else -> 1.0f
            }

            val texture = SpriteTextureManager.getTexture(enemy.type, enemy.state)
            spriteShader.draw(modelMatrix, viewMatrix, projectionMatrix, texture, alpha)
        }
    }

    private fun billboardMatrix(modelMatrix: FloatArray, camera: Camera) {
        val cameraPos = FloatArray(3)
        camera.displayOrientedPose.getTranslation(cameraPos, 0)
        val enemyPos = floatArrayOf(modelMatrix[12], modelMatrix[13], modelMatrix[14])
        val dx = cameraPos[0] - enemyPos[0]
        val dz = cameraPos[2] - enemyPos[2]
        val angle = Math.atan2(dx.toDouble(), dz.toDouble()).toFloat()
        android.opengl.Matrix.rotateM(modelMatrix, 0, Math.toDegrees(angle.toDouble()).toFloat(), 0f, 1f, 0f)
    }

    private fun renderEffects(camera: Camera) {
        if (muzzleFlashTimer > 0) {
            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            camera.getViewMatrix(viewMatrix, 0)

            val modelMatrix = FloatArray(16)
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)

            val cameraPos = FloatArray(3)
            camera.displayOrientedPose.getTranslation(cameraPos, 0)
            android.opengl.Matrix.translateM(modelMatrix, 0, cameraPos[0], cameraPos[1], cameraPos[2] - 0.5f)

            val flashScale = muzzleFlashTimer * 5f
            android.opengl.Matrix.scaleM(modelMatrix, 0, flashScale, flashScale, flashScale)

            effectShader.drawMuzzleFlash(modelMatrix, viewMatrix, projectionMatrix)
            muzzleFlashTimer -= 0.016f
        }
    }
}
