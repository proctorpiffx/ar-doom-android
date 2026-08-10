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
 */
class ARCameraManager(
    private val context: Context,
    val session: Session
) {
    private var displayRotation: Int = 0
    private var viewportWidth: Int = 1920
    private var viewportHeight: Int = 1080

    init {
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

    fun acquireFrame(): Frame? {
        return try {
            session.update()
        } catch (e: Exception) {
            null
        }
    }

    fun isTracking(frame: Frame): Boolean {
        return frame.camera.trackingState == TrackingState.TRACKING
    }

    fun getCameraPose(frame: Frame): Camera {
        return frame.camera
    }

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
