package com.ardoom.input

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ardoom.game.GameEngine

/**
 * Handles all player input for AR DOOM:
 *
 * - Tap: Fire weapon
 * - Long press: Switch weapon
 * - Swipe left/right: Switch between available weapons
 * - Swipe down: Reload / pick up items
 *
 * The player physically moves and looks around in real space via ARCore
 * tracking — the phone IS the controller.
 */
class TouchController(
    private val gameEngine: GameEngine,
    private val onFire: (Float, Float) -> Unit,
    private val onWeaponSwitch: (Int) -> Unit
) : View.OnTouchListener {

    private var gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(gameEngine.javaClass.classLoader?.let {
            GestureDetector.SimpleOnGestureListener()
        }?.let {
            GestureDetector.SimpleOnGestureListener()
        }?.let {
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onFire(e.x, e.y)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    // Cycle to next weapon
                    val weapons = com.ardoom.game.Weapon.values()
                    val currentIndex = weapons.indexOf(gameEngine.currentWeapon)
                    val nextIndex = (currentIndex + 1) % weapons.size
                    gameEngine.currentWeapon = weapons[nextIndex]
                    onWeaponSwitch(nextIndex)
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
                        val direction = if (dx > 0) 1 else -1
                        onWeaponSwitch(direction)
                    } else if (dy > 100) {
                        // Swipe down — reload/pickup
                        gameEngine.ammo += 10
                    }
                    return true
                }
            }
        } ?: GestureDetector.SimpleOnGestureListener().let {
            object : GestureDetector.SimpleOnGestureListener() {}
        })
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event ?: return false)
    }
}
