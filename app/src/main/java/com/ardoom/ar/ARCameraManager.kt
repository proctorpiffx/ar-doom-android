package com.ardoom.ar

import android.content.Context
import android.opengl.GLES30
import android.view.WindowManager
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

/**
 * Manages the ARCore camera lifecycle, frame updates, and provides
 * the current camera pose + display rotation to the renderer.
 *
 * On the Galaxy S25, ARCore delivers excellent 6-DOF tracking with
 * the built-in depth sensor. We use depth to spawn enemies at real
 * surfaces in front of the player.
 */
class ARCameraManager(
    private val context: Context,
    private val session: Session
) {
    private var displayRotation: Int = 0
    private var viewportWidth: Int = 1920
    private var viewportHeight: Int = 1080

    init {
        // Lock to landscape (DOOM is meant to be played wide)
        updateDisplayRotation()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        session.setDisplayGeometry(displayRotation, width, height)
    }

    fun updateDisplayRotation() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        displayRotation = windowManager.defaultDisplay.rotation
        session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
    }

    /**
     * Acquire the latest AR frame from ARCore.
     * Returns null if tracking is lost (we pause enemy spawning during loss).
     */
    fun acquireFrame(): Frame? {
        return try {
            session.update()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if we have valid tracking — used to decide whether
     * enemies can spawn and the player can take damage.
     */
    fun isTracking(frame: Frame): Boolean {
        return frame.camera.trackingState == TrackingState.TRACKING
    }

    /**
     * Returns the camera pose in AR world space.
     * The player's "position" in the game maps to this pose.
     */
    fun getCameraPose(frame: Frame): Camera {
        return frame.camera
    }

    /**
     * Raycast from screen center to find a surface point where
     * we can spawn a DOOM enemy. Uses ARCore depth + hit testing.
     */
    fun raycastToSurface(frame: Frame, screenX: Float, screenY: Float): com.google.ar.core.Pose? {
        val hits = frame.hitTest(screenX, screenY)
        if (hits.isEmpty()) return null

        val hit = hits.first()
        val trackable = hit.trackable
        if (trackable is com.google.ar.core.Plane || trackable is com.google.ar.core.Point) {
            return hit.hitPose
        }
        return null
    }
}
