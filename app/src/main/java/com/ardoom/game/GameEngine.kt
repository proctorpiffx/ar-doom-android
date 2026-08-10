package com.ardoom.game

import android.content.Context
import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.Pose

/**
 * The DOOM game engine — drives the game loop, enemy spawning,
 * combat logic, health/ammo tracking, and score.
 */
class GameEngine(private val context: Context) {

    // Player state
    var health: Int = 100
    var armor: Int = 0
    var ammo: Int = 50
    var score: Int = 0
    var currentWeapon: Weapon = Weapon.PISTOL

    // Flags for the renderer to read
    var playerWasHit: Boolean = false
    var enemyDiedThisFrame: Boolean = false

    // Enemy management
    private val enemies = mutableListOf<Enemy>()

    // Game state
    var gameState: GameState = GameState.READY
    var waveNumber: Int = 1
    private var enemiesPerWave: Int = 3
    private var enemiesSpawnedThisWave: Int = 0
    private var lastSpawnTime: Long = 0
    private var lastDamageTime: Long = 0

    fun startGame() {
        health = 100
        armor = 0
        ammo = 50
        score = 0
        currentWeapon = Weapon.PISTOL
        waveNumber = 1
        enemiesPerWave = 3
        enemiesSpawnedThisWave = 0
        enemies.clear()
        gameState = GameState.PLAYING
        lastSpawnTime = System.currentTimeMillis()
        Log.i(TAG, "Game started — wave 1")
    }

    fun update(deltaTime: Float, camera: Camera, arManager: com.ardoom.ar.ARCameraManager, frame: com.google.ar.core.Frame) {
        if (gameState != GameState.PLAYING) return

        val now = System.currentTimeMillis()

        // Spawn enemies
        if (shouldSpawn(now) && enemiesSpawnedThisWave < enemiesPerWave && enemies.size < 6) {
            spawnEnemyInFrontOfPlayer(camera)
            enemiesSpawnedThisWave++
        }

        // Update enemies
        updateEnemies(deltaTime, camera, now)

        // Check wave progression
        if (enemiesSpawnedThisWave >= enemiesPerWave && enemies.isEmpty() && waveNumber < 50) {
            startNextWave()
        }

        // Check death
        if (health <= 0) {
            health = 0
            gameState = GameState.GAME_OVER
            Log.i(TAG, "Player died. Score: $score, Wave: $waveNumber")
        }
    }

    fun fire(screenX: Float, screenY: Float, arManager: com.ardoom.ar.ARCameraManager, frame: com.google.ar.core.Frame) {
        if (ammo <= 0) {
            Log.i(TAG, "Out of ammo!")
            return
        }

        ammo--
        val camPose = frame.camera.displayOrientedPose
        shootEnemiesInView(camPose)
        Log.i(TAG, "Fired ${currentWeapon.name} — ammo: $ammo")
    }

    private fun shootEnemiesInView(camPose: Pose) {
        val cameraPos = FloatArray(3)
        camPose.getTranslation(cameraPos, 0)

        // Camera forward vector from quaternion
        val quat = FloatArray(4)
        camPose.getRotationQuaternion(quat, 0)
        val forward = rotateVector(floatArrayOf(0f, 0f, -1f), quat)

        val toRemove = mutableListOf<Enemy>()
        for (enemy in enemies) {
            if (enemy.state == EnemyState.DEAD || enemy.state == EnemyState.DYING) continue

            val dx = enemy.position[0] - cameraPos[0]
            val dy = enemy.position[1] - cameraPos[1]
            val dz = enemy.position[2] - cameraPos[2]
            val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

            if (distance < MAX_FIRE_RANGE && distance > 0.1f) {
                val dot = (dx * forward[0] + dy * forward[1] + dz * forward[2]) / distance
                if (dot > 0.65f) {
                    enemy.takeDamage(currentWeapon.damage)
                    if (!enemy.isAlive()) {
                        score += enemy.scoreValue
                        enemyDiedThisFrame = true
                        toRemove.add(enemy)
                    }
                }
            }
        }
        enemies.removeAll(toRemove)
    }

    private fun rotateVector(v: FloatArray, q: FloatArray): FloatArray {
        // Quaternion rotation: v' = q * v * q^-1
        val qx = q[0]; val qy = q[1]; val qz = q[2]; val qw = q[3]
        val vx = v[0]; val vy = v[1]; val vz = v[2]
        return floatArrayOf(
            vx + 2f * (qx * vz * vy - qy * vz - qz * vy + qw * vx) * 0f + // simplified
            (1f - 2f * (qy * qy + qz * qz)) * vx + 2f * (qx * qy - qw * qz) * vy + 2f * (qx * qz + qw * qy) * vz,
            2f * (qx * qy + qw * qz) * vx + (1f - 2f * (qx * qx + qz * qz)) * vy + 2f * (qy * qz - qw * qx) * vz,
            2f * (qx * qz - qw * qy) * vx + 2f * (qy * qz + qw * qx) * vy + (1f - 2f * (qx * qx + qy * qy)) * vz
        )
    }

    private fun spawnEnemyInFrontOfPlayer(camera: Camera) {
        val cameraPose = camera.displayOrientedPose
        val cameraPos = FloatArray(3)
        cameraPose.getTranslation(cameraPos, 0)

        // Get camera forward direction
        val quat = FloatArray(4)
        cameraPose.getRotationQuaternion(quat, 0)
        val forward = rotateVector(floatArrayOf(0f, 0f, -1f), quat)

        val distance = 2.0f + Math.random().toFloat() * 3.0f // 2-5 meters
        val lateralOffset = (-1.5f + Math.random().toFloat() * 3.0f) // ±1.5m spread
        val right = rotateVector(floatArrayOf(1f, 0f, 0f), quat)

        val spawnX = cameraPos[0] + forward[0] * distance + right[0] * lateralOffset
        val spawnY = cameraPos[1] + forward[1] * distance
        val spawnZ = cameraPos[2] + forward[2] * distance

        val enemyType = when {
            waveNumber < 3 -> if (Math.random() < 0.6) EnemyType.SOLDIER else EnemyType.IMP
            waveNumber < 6 -> {
                val r = Math.random()
                when {
                    r < 0.4 -> EnemyType.SOLDIER
                    r < 0.75 -> EnemyType.IMP
                    else -> EnemyType.DEMON
                }
            }
            else -> {
                val r = Math.random()
                when {
                    r < 0.2 -> EnemyType.SOLDIER
                    r < 0.5 -> EnemyType.IMP
                    r < 0.75 -> EnemyType.DEMON
                    else -> if (r < 0.9) EnemyType.CACODEMON else EnemyType.BARON
                }
            }
        }

        val enemy = Enemy(
            type = enemyType,
            position = floatArrayOf(spawnX, spawnY, spawnZ)
        )
        enemies.add(enemy)
        Log.i(TAG, "Spawned ${enemyType.name} at ~${distance}m — wave $waveNumber (${enemiesSpawnedThisWave}/${enemiesPerWave})")
    }

    private fun updateEnemies(deltaTime: Float, camera: Camera, now: Long) {
        val cameraPose = camera.displayOrientedPose
        val cameraPos = FloatArray(3)
        cameraPose.getTranslation(cameraPos, 0)

        val toRemove = mutableListOf<Enemy>()
        for (enemy in enemies) {
            enemy.update(deltaTime, cameraPos)

            if (enemy.distanceToPlayer(cameraPos) < ENEMY_ATTACK_RANGE && now - lastDamageTime > 800) {
                val damage = enemy.type.attackDamage
                if (armor > 0) {
                    val absorbed = minOf(armor, damage / 3)
                    armor -= absorbed
                    health -= (damage - absorbed)
                } else {
                    health -= damage
                }
                lastDamageTime = now
                playerWasHit = true
                Log.i(TAG, "Hit by ${enemy.type.name} for $damage — HP: $health")
            }

            if (!enemy.isAlive() && enemy.state == EnemyState.DEAD) {
                toRemove.add(enemy)
            }
        }
        enemies.removeAll(toRemove)
    }

    private fun shouldSpawn(now: Long): Boolean =
        now - lastSpawnTime > SPAWN_INTERVAL_MS

    private fun startNextWave() {
        waveNumber++
        enemiesPerWave += 2
        enemiesSpawnedThisWave = 0
        ammo += 20
        if (health < 100) health = minOf(100, health + 25)
        if (waveNumber % 3 == 0) armor = minOf(100, armor + 25)
        lastSpawnTime = System.currentTimeMillis()
        Log.i(TAG, "Wave $waveNumber — enemies: $enemiesPerWave, HP: $health, ammo: $ammo")
    }

    fun getEnemyCount(): Int = enemies.size
    fun getEnemies(): List<Enemy> = enemies

    companion object {
        private const val TAG = "GameEngine"
        private const val MAX_FIRE_RANGE = 15.0f
        private const val ENEMY_ATTACK_RANGE = 1.5f
        private const val SPAWN_INTERVAL_MS = 2000L
    }
}

enum class GameState {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER,
    VICTORY
}

enum class Weapon(val damage: Int, val fireRateMs: Long, val displayName: String) {
    PISTOL(damage = 25, fireRateMs = 300, displayName = "PISTOL"),
    SHOTGUN(damage = 60, fireRateMs = 800, displayName = "SHOTGUN"),
    CHAINGUN(damage = 20, fireRateMs = 100, displayName = "CHAINGUN"),
    PLASMA(damage = 80, fireRateMs = 500, displayName = "PLASMA"),
    BFG(damage = 500, fireRateMs = 1500, displayName = "BFG")
}

data class Projectile(
    var position: FloatArray,
    var velocity: FloatArray,
    var damage: Int,
    var life: Float = 2.0f
) {
    fun update(deltaTime: Float) {
        position[0] += velocity[0] * deltaTime
        position[1] += velocity[1] * deltaTime
        position[2] += velocity[2] * deltaTime
        life -= deltaTime
    }
}
