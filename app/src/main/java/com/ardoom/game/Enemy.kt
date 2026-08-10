package com.ardoom.game

import android.util.Log

/**
 * Enemy entities that spawn in AR space and move toward the player.
 * Each enemy type has different health, speed, damage, and score value,
 * mirroring classic DOOM enemy archetypes.
 */
class Enemy(
    val type: EnemyType,
    var position: FloatArray
) {
    var health: Int = type.maxHealth
    var state: EnemyState = EnemyState.IDLE
    private var stateTimer: Float = 0f

    fun update(deltaTime: Float, playerPosition: FloatArray) {
        val dx = playerPosition[0] - position[0]
        val dy = playerPosition[1] - position[1]
        val dz = playerPosition[2] - position[2]
        val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

        when (state) {
            EnemyState.IDLE -> {
                stateTimer += deltaTime
                if (distance < DETECTION_RANGE) {
                    state = EnemyState.CHASING
                    stateTimer = 0f
                }
            }
            EnemyState.CHASING -> {
                // Move toward player
                if (distance > 0.1f) {
                    val speed = type.moveSpeed * deltaTime
                    position[0] += (dx / distance) * speed
                    position[1] += (dy / distance) * speed * 0.5f  // less vertical movement
                    position[2] += (dz / distance) * speed
                }
                if (distance < ATTACK_RANGE) {
                    state = EnemyState.ATTACKING
                    stateTimer = 0f
                }
            }
            EnemyState.ATTACKING -> {
                stateTimer += deltaTime
                if (distance > ATTACK_RANGE * 1.5f) {
                    state = EnemyState.CHASING
                    stateTimer = 0f
                }
                // Attack handled by GameEngine which checks state + timing
            }
            EnemyState.HURT -> {
                stateTimer += deltaTime
                if (stateTimer > 0.3f) {
                    state = EnemyState.CHASING
                    stateTimer = 0f
                }
            }
            EnemyState.DYING -> {
                stateTimer += deltaTime
                if (stateTimer > 1.0f) {
                    state = EnemyState.DEAD
                }
            }
            EnemyState.DEAD -> { /* no-op */ }
        }

        Log.v(TAG, "${type.name} at (${position[0]}, ${position[2]}) dist=${distance} state=$state")
    }

    fun takeDamage(damage: Int) {
        health -= damage
        Log.i(TAG, "${type.name} took $damage damage — HP: $health")
        if (health <= 0) {
            health = 0
            state = EnemyState.DYING
            stateTimer = 0f
        } else if (state != EnemyState.DYING) {
            state = EnemyState.HURT
            stateTimer = 0f
        }
    }

    fun isAlive(): Boolean = state != EnemyState.DEAD && state != EnemyState.DYING

    fun distanceToPlayer(playerPos: FloatArray): Float {
        val dx = position[0] - playerPos[0]
        val dy = position[1] - playerPos[1]
        val dz = position[2] - playerPos[2]
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    val scoreValue: Int
        get() = type.scoreValue

    companion object {
        private const val TAG = "Enemy"
        private const val DETECTION_RANGE = 10.0f
        private const val ATTACK_RANGE = 1.5f
    }
}

enum class EnemyType(
    val maxHealth: Int,
    val moveSpeed: Float,
    val attackDamage: Int,
    val scoreValue: Int
) {
    IMP(maxHealth = 60, moveSpeed = 2.5f, attackDamage = 20, scoreValue = 100),
    SOLDIER(maxHealth = 30, moveSpeed = 1.8f, attackDamage = 10, scoreValue = 50),
    DEMON(maxHealth = 150, moveSpeed = 1.5f, attackDamage = 40, scoreValue = 250),
    CACODEMON(maxHealth = 200, moveSpeed = 2.0f, attackDamage = 35, scoreValue = 400),
    BARON(maxHealth = 300, moveSpeed = 1.2f, attackDamage = 50, scoreValue = 500)
}

enum class EnemyState {
    IDLE,
    CHASING,
    ATTACKING,
    HURT,
    DYING,
    DEAD
}
