package com.ardoom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ardoom.ar.ARCameraManager
import com.ardoom.audio.AudioManager
import com.ardoom.game.GameEngine
import com.ardoom.game.GameState
import com.ardoom.game.Weapon
import com.ardoom.rendering.DoomRenderer
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var hudContainer: View
    private lateinit var hudHealth: TextView
    private lateinit var hudAmmo: TextView
    private lateinit var hudScore: TextView
    private lateinit var hudWave: TextView
    private lateinit var hudWeapon: TextView
    private lateinit var hudMessage: TextView

    private lateinit var cameraManager: ARCameraManager
    private lateinit var renderer: DoomRenderer
    private lateinit var gameEngine: GameEngine
    private lateinit var audioManager: AudioManager
    private lateinit var hapticManager: HapticManager

    private var arSession: Session? = null
    private var userRequestedInstall = true
    private var installRequested = false

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build the view hierarchy: GL surface + HUD overlay
        val rootLayout = FrameLayout(this)

        glSurfaceView = GLSurfaceView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        hudContainer = layoutInflater.inflate(R.layout.activity_main, null).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        hudHealth = hudContainer.findViewById(R.id.hud_health)
        hudAmmo = hudContainer.findViewById(R.id.hud_ammo)
        hudScore = hudContainer.findViewById(R.id.hud_score)
        hudWave = hudContainer.findViewById(R.id.hud_wave)
        hudWeapon = hudContainer.findViewById(R.id.hud_weapon)
        hudMessage = hudContainer.findViewById(R.id.hud_message)

        rootLayout.addView(glSurfaceView)
        rootLayout.addView(hudContainer)
        setContentView(rootLayout)

        // Initialize sub-systems
        gameEngine = GameEngine(this)
        audioManager = AudioManager(this)
        hapticManager = HapticManager(this)

        // Set up touch controls
        setupTouchControls()

        // Check ARCore, then request camera permission
        checkARCoreSupport()

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initAR()
        }
    }

    private fun checkARCoreSupport() {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    Log.i(TAG, "ARCore is installed and ready")
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    Log.i(TAG, "ARCore needs install/update — will prompt at onResume")
                }
                else -> {
                    showError(getString(R.string.arcore_unsupported))
                    Log.e(TAG, "ARCore not supported: $availability")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ARCore availability check failed", e)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun initAR() {
        try {
            // Request ARCore installation if needed
            val installStatus = ArCoreApk.getInstance().requestInstall(
                this, userRequestedInstall
            )
            when (installStatus) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    Log.i(TAG, "ARCore install requested — will resume after install")
                    return
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                    Log.i(TAG, "ARCore installed, continuing")
                }
                null -> {
                    Log.i(TAG, "ARCore install status null, continuing")
                }
            }

            // Create and configure ARCore session
            val session = Session(this)
            val config = Config(session).apply {
                // Enable depth for better surface detection (S25 has depth sensor)
                depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                // Enable instant placement for faster enemy spawning
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            }

            session.configure(config)
            arSession = session

            // Initialize camera manager with the session
            cameraManager = ARCameraManager(this, session)

            // Initialize the renderer
            renderer = DoomRenderer(this, cameraManager, gameEngine)
            renderer.setCallbacks(
                onFire = { audioManager.playWeaponSound(gameEngine.currentWeapon); hapticManager.fire() },
                onHit = { hapticManager.hit(); audioManager.playSound("enemy_die") },
                onPlayerHit = { hapticManager.damage(); audioManager.playSound("player_hurt") },
                onWaveStart = { wave -> showWaveMessage(wave) }
            )

            glSurfaceView.apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }

            // Load audio
            audioManager.loadSounds()

            // Start the game
            gameEngine.startGame()
            showWaveMessage(1)

            Log.i(TAG, "AR session initialized — let's DOOM")
        } catch (e: UnavailableException) {
            Log.e(TAG, "ARCore unavailable", e)
            showError(getString(R.string.ar_init_failed))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init AR", e)
            showError(getString(R.string.ar_init_failed))
        }
    }

    private fun setupTouchControls() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (gameEngine.gameState != GameState.PLAYING) return false
                renderer.fire(e.x, e.y)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                cycleWeapon()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y

                if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                    // Horizontal swipe — weapon switch
                    cycleWeapon()
                } else if (dy > 150 && kotlin.math.abs(velocityY) > 500) {
                    // Swipe down — reload
                    gameEngine.ammo += 15
                    audioManager.playSound("pickup")
                    updateHUD()
                }
                return true
            }
        })

        glSurfaceView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun cycleWeapon() {
        val weapons = Weapon.values()
        val currentIdx = weapons.indexOf(gameEngine.currentWeapon)
        val nextIdx = (currentIdx + 1) % weapons.size
        gameEngine.currentWeapon = weapons[nextIdx]
        audioManager.playSound("pickup")
        hapticManager.tick()
        updateHUD()
    }

    private fun showWaveMessage(wave: Int) {
        runOnUiThread {
            hudMessage.text = getString(R.string.wave_start, wave)
            hudMessage.visibility = View.VISIBLE
            hudMessage.postDelayed({ hudMessage.visibility = View.GONE }, 2000)
        }
    }

    fun updateHUD() {
        runOnUiThread {
            hudHealth.text = getString(R.string.hud_health_label) + ": " + gameEngine.health
            hudAmmo.text = getString(R.string.hud_ammo_label) + ": " + gameEngine.ammo
            hudScore.text = getString(R.string.hud_score_label) + ": " + gameEngine.score
            hudWave.text = getString(R.string.hud_wave_label) + ": " + gameEngine.waveNumber
            hudWeapon.text = gameEngine.currentWeapon.name

            // Color health based on level
            hudHealth.setTextColor(
                when {
                    gameEngine.health > 60 -> 0xFF00FF00.toInt()
                    gameEngine.health > 30 -> 0xFFFFFF00.toInt()
                    else -> 0xFFFF0000.toInt()
                }
            )

            if (gameEngine.gameState == GameState.GAME_OVER) {
                hudMessage.text = getString(R.string.game_over, gameEngine.score, gameEngine.waveNumber)
                hudMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            hudMessage.text = message
            hudMessage.visibility = View.VISIBLE
            hudMessage.setTextColor(0xFFFF0000.toInt())
        }
    }

    override fun onResume() {
        super.onResume()

        // If we were waiting for ARCore install, try to init now
        if (arSession == null && hasCameraPermission() && installRequested) {
            installRequested = false
            initAR()
        }

        arSession?.resume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        arSession?.close()
        arSession = null
        audioManager.release()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAR()
            } else {
                showError(getString(R.string.camera_denied))
            }
        }
    }
}
