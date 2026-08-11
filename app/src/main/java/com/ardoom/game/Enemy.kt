package com.ardoom.game

import android.util.Log

/**
 * Enemy entities that spawn in AR space and move toward the player.
 * Each enemy type has different health, speed, damage, attack cooldown,
 * and score value, mirroring classic DOOM enemy archetypes.
 */
class Enemy(
    val type: EnemyType,
    var position: FloatArray
) {
    var maxHealth: Int = type.maxHealth
    var health: Int = type.maxHealth
    var state: EnemyState = EnemyState.IDLE
    private var stateTimer: Float = 0f

    // Animations & timings
    var spawnTimer: Float = 0f
    var hurtTimer: Float = 0f
    var attackTimer: Float = 0f
    var currentScale: Float = 0f
    var alpha: Float = 1.0f

    private var bobPhase: Float = (Math.random() * Math.PI * 2).toFloat()

    fun update(deltaTime: Float, playerPosition: FloatArray) {
        // Spawn animation: scale up from 0 to 1 over SPAWN_DURATION
        if (spawnTimer < SPAWN_DURATION) {
            spawnTimer += deltaTime
            currentScale = (spawnTimer / SPAWN_DURATION).coerceIn(0f, 1f)
        } else if (state != EnemyState.DYING) {
            currentScale = 1.0f
        }

        // Timers
        if (hurtTimer > 0f) hurtTimer -= deltaTime
        if (attackTimer > 0f) attackTimer -= deltaTime
        bobPhase += deltaTime * 2f

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
                // Move toward player with type-based movement variation
                if (distance > 0.1f) {
                    val speed = type.moveSpeed * deltaTime
                    val dirX = dx / distance
                    val dirY = dy / distance
                    val dirZ = dz / distance

                    // Apply type-based movement variation
                    when (type) {
                        EnemyType.IMP -> {
                            // Fast, subtle lateral weave
                            val lateral = Math.cos(bobPhase.toDouble() * 3.0).toFloat() * 0.3f * speed
                            position[0] += dirX * speed + (-dirZ * lateral)
                            position[1] += dirY * speed * 0.3f
                            position[2] += dirZ * speed + (dirX * lateral)
                        }
                        EnemyType.CACODEMON -> {
                            // Floating vertical sine-wave bob
                            val floatBob = Math.sin(bobPhase.toDouble() * 2.0).toFloat() * 0.2f * deltaTime
                            position[0] += dirX * speed
                            position[1] += dirY * speed * 0.5f + floatBob
                            position[2] += dirZ * speed
                        }
                        EnemyType.BARON -> {
                            // Slow, steady march
                            position[0] += dirX * speed
                            position[1] += dirY * speed * 0.2f
                            position[2] += dirZ * speed
                        }
                        else -> {
                            position[0] += dirX * speed
                            position[1] += dirY * speed * 0.5f
                            position[2] += dirZ * speed
                        }
                    }
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
                // Attack handled by GameEngine which checks state + timing + attackTimer
            }
            EnemyState.HURT -> {
                stateTimer += deltaTime
                if (stateTimer > HURT_DURATION) {
                    state = EnemyState.CHASING
                    stateTimer = 0f
                }
            }
            EnemyState.DYING -> {
                stateTimer += deltaTime
                // Death animation: fade out + shrink
                val progress = (stateTimer / DEATH_DURATION).coerceIn(0f, 1f)
                alpha = 1.0f - progress
                currentScale = 1.0f - progress * 0.5f

                if (stateTimer >= DEATH_DURATION) {
                    state = EnemyState.DEAD
                    alpha = 0.0f
                    currentScale = 0.0f
                }
            }
            EnemyState.DEAD -> {
                alpha = 0.0f
                currentScale = 0.0f
            }
        }

        Log.v(TAG, "${type.name} at (${position[0]}, ${position[2]}) dist=${distance} state=$state")
    }

    fun takeDamage(damage: Int) {
        health -= damage
        hurtTimer = HURT_DURATION
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

    fun canAttack(): Boolean = attackTimer <= 0f

    fun resetAttackCooldown() {
        attackTimer = type.attackCooldown
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
        private const val SPAWN_DURATION = 0.4f
        private const val HURT_DURATION = 0.3f
        private const val DEATH_DURATION = 0.8f
    }
}

enum class EnemyType(
    val maxHealth: Int,
    val moveSpeed: Float,
    val attackDamage: Int,
    val scoreValue: Int,
    val attackCooldown: Float = 1.5f
) {
    IMP(maxHealth = 60, moveSpeed = 2.5f, attackDamage = 20, scoreValue = 100, attackCooldown = 1.5f),
    SOLDIER(maxHealth = 30, moveSpeed = 1.8f, attackDamage = 10, scoreValue = 50, attackCooldown = 2.0f),
    DEMON(maxHealth = 150, moveSpeed = 1.5f, attackDamage = 40, scoreValue = 250, attackCooldown = 1.0f),
    CACODEMON(maxHealth = 200, moveSpeed = 2.0f, attackDamage = 35, scoreValue = 400, attackCooldown = 2.2f),
    BARON(maxHealth = 300, moveSpeed = 1.2f, attackDamage = 50, scoreValue = 500, attackCooldown = 2.5f)
}

enum class EnemyState {
    IDLE,
    CHASING,
    ATTACKING,
    HURT,
    DYING,
    DEAD
}
