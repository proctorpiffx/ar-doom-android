package com.ardoom

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ardoom.ar.ARCameraManager
import com.ardoom.game.GameEngine
import com.ardoom.rendering.DoomRenderer
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session

class MainActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var hudOverlay: TextView
    private lateinit var cameraManager: ARCameraManager
    private lateinit var renderer: DoomRenderer
    private lateinit var gameEngine: GameEngine

    private var arSession: Session? = null
    private var userRequestedInstall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up fullscreen landscape AR view
        glSurfaceView = GLSurfaceView(this)
        hudOverlay = TextView(this).apply {
            text = getString(R.string.hud_health_label) + ": 100"
            setTextColor(0xFFFF4444.toInt())
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }

        setContentView(glSurfaceView)

        // Check ARCore availability on this device (S25 fully supported)
        checkARCoreSupport()

        // Request camera permission
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            initAR()
        }
    }

    private fun checkARCoreSupport() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                Log.i(TAG, "ARCore is installed and ready")
            }
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                Log.i(TAG, "ARCore needs install/update — will prompt user")
            }
            else -> {
                Toast.makeText(this, getString(R.string.arcore_unsupported), Toast.LENGTH_LONG).show()
                Log.e(TAG, "ARCore not supported: $availability")
            }
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun initAR() {
        try {
            // Create ARCore session
            val config = com.google.ar.core.Config(arSession)
            config.depthMode = com.google.ar.core.Config.DepthMode.AUTOMATIC
            config.lightEstimationMode = com.google.ar.core.Config.LightEstimationMode.ENVIRONMENTAL_HDR

            arSession = Session(this).apply {
                configure(config)
            }

            cameraManager = ARCameraManager(this, arSession!!)
            gameEngine = GameEngine(this)
            renderer = DoomRenderer(this, cameraManager, gameEngine)

            glSurfaceView.apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }

            Log.i(TAG, "AR session initialized — let's DOOM")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init AR", e)
            Toast.makeText(this, getString(R.string.ar_init_failed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
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
                Toast.makeText(this, getString(R.string.camera_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_CODE = 1001
    }
}
