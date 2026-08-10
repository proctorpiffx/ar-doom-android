package com.ardoom.game

import android.content.Context
import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.Pose

/**
 * The DOOM game engine — drives the game loop, enemy spawning,
 * combat logic, health/ammo tracking, and score.
 *
 * In AR mode, the player physically moves/looks around in real space.
 * The phone is the weapon — tap to shoot. Enemies spawn on detected
 * surfaces in front of the player.
 */
class GameEngine(private val context: Context) {

    // Player state
    var health: Int = 100
    var armor: Int = 0
    var ammo: Int = 50
    var score: Int = 0
    var currentWeapon: Weapon = Weapon.PISTOL

    // Enemy management
    private val enemies = mutableListOf<Enemy>()
    private val projectiles = mutableListOf<Projectile>()

    // Game state
    var gameState: GameState = GameState.PLAYING
    var waveNumber: Int = 1
    var enemiesPerWave: Int = 3
    private var lastSpawnTime: Long = 0
    private var lastDamageTime: Long = 0

    fun update(deltaTime: Float, cameraPose: Camera, arManager: com.ardoom.ar.ARCameraManager, frame: com.google.ar.core.Frame) {
        if (gameState != GameState.PLAYING) return

        val now = System.currentTimeMillis()

        // Spawn enemies on surfaces in front of the player
        if (shouldSpawn(now) && enemies.size < 5) {
            spawnEnemyInFrontOfPlayer(cameraPose)
        }

        // Update all enemies — move toward player, attempt attacks
        updateEnemies(deltaTime, cameraPose, now)

        // Update projectiles
        updateProjectiles(deltaTime)

        // Check wave progression
        if (enemies.isEmpty() && waveNumber < 20) {
            startNextWave()
        }

        // Check death
        if (health <= 0) {
            gameState = GameState.GAME_OVER
            Log.i(TAG, "Player died. Final score: $score, reached wave $waveNumber")
        }
    }

    /**
     * Fire the current weapon from screen center tap.
     * Raycasts into AR space; if it hits an enemy, deals damage.
     */
    fun fire(screenX: Float, screenY: Float, arManager: com.ardoom.ar.ARCameraManager, frame: com.google.ar.core.Frame) {
        if (ammo <= 0) {
            Log.i(TAG, "Out of ammo!")
            return
        }

        ammo--
        val hitPose = arManager.raycastToSurface(frame, screenX, screenY)

        if (hitPose != null) {
            // Check if any enemy is near the hit point
            val hitPoint = floatArrayOf(0f, 0f, 0f)
            hitPose.getTranslation(hitPoint, 0)

            val nearestEnemy = findNearestEnemyTo(hitPoint)
            nearestEnemy?.takeDamage(currentWeapon.damage)
        } else {
            // Even without surface hit, check enemies in view direction
            val camPose = frame.camera.displayOrientedPose
            shootEnemiesInView(camPose)
        }

        // Haptic feedback handled by activity
        Log.i(TAG, "Fired ${currentWeapon.name} — ammo: $ammo")
    }

    private fun shootEnemiesInView(camPose: Pose) {
        val cameraPos = FloatArray(3)
        camPose.getTranslation(cameraPos, 0)
        val forward = floatArrayOf(0f, 0f, -1f)
        // Transform forward by camera rotation
        val quat = FloatArray(4)
        camPose.getRotationQuaternion(quat, 0)
        // Simple distance check: enemies within a cone in front
        val enemiesToRemove = mutableListOf<Enemy>()
        for (enemy in enemies) {
            val dx = enemy.position[0] - cameraPos[0]
            val dy = enemy.position[1] - cameraPos[1]
            val dz = enemy.position[2] - cameraPos[2]
            val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
            if (distance < MAX_FIRE_RANGE) {
                // Simple: if enemy is within range and roughly in front, hit it
                val dot = (dx * forward[0] + dy * forward[1] + dz * forward[2]) / distance
                if (dot > 0.7f) {
                    enemy.takeDamage(currentWeapon.damage)
                    if (!enemy.isAlive()) {
                        score += enemy.scoreValue
                        enemiesToRemove.add(enemy)
                    }
                }
            }
        }
        enemies.removeAll(enemiesToRemove)
    }

    private fun spawnEnemyInFrontOfPlayer(camera: Camera) {
        val cameraPose = camera.displayOrientedPose
        val cameraPos = FloatArray(3)
        cameraPose.getTranslation(cameraPos, 0)

        // Spawn 1.5 - 4 meters in front of the player
        val distance = (1.5f + Math.random().toFloat() * 2.5f)
        val angle = (-30 + Math.random() * 60) * Math.PI / 180  // ±30 degrees
        val spawnX = cameraPos[0] + (distance * Math.cos(angle)).toFloat()
        val spawnY = cameraPos[1] // Same height as camera
        val spawnZ = cameraPos[2] - distance

        val enemyType = when ((Math.random() * 100).toInt() % 3) {
            0 -> EnemyType.IMP
            1 -> EnemyType.SOLDIER
            else -> EnemyType.DEMON
        }

        val enemy = Enemy(
            type = enemyType,
            position = floatArrayOf(spawnX, spawnY, spawnZ)
        )
        enemies.add(enemy)
        Log.i(TAG, "Spawned ${enemyType.name} at distance ${distance}m — wave $waveNumber")
    }

    private fun updateEnemies(deltaTime: Float, camera: Camera, now: Long) {
        val cameraPose = camera.displayOrientedPose
        val cameraPos = FloatArray(3)
        cameraPose.getTranslation(cameraPos, 0)

        val toRemove = mutableListOf<Enemy>()
        for (enemy in enemies) {
            enemy.update(deltaTime, cameraPos)

            // Enemy attacks if close enough
            if (enemy.distanceToPlayer(cameraPos) < ENEMY_ATTACK_RANGE && now - lastDamageTime > 1000) {
                val damage = enemy.type.attackDamage
                if (armor > 0) {
                    val absorbed = minOf(armor, damage / 3)
                    armor -= absorbed
                    health -= (damage - absorbed)
                } else {
                    health -= damage
                }
                lastDamageTime = now
                Log.i(TAG, "Player hit by ${enemy.type.name} for $damage — health: $health")
            }

            if (!enemy.isAlive()) {
                toRemove.add(enemy)
            }
        }
        enemies.removeAll(toRemove)
    }

    private fun updateProjectiles(deltaTime: Float) {
        val toRemove = mutableListOf<Projectile>()
        for (proj in projectiles) {
            proj.update(deltaTime)
            if (proj.life <= 0) toRemove.add(proj)
        }
        projectiles.removeAll(toRemove)
    }

    private fun findNearestEnemyTo(point: FloatArray): Enemy? {
        var nearest: Enemy? = null
        var minDist = Float.MAX_VALUE

        for (enemy in enemies) {
            if (!enemy.isAlive()) continue
            val dx = enemy.position[0] - point[0]
            val dy = enemy.position[1] - point[1]
            val dz = enemy.position[2] - point[2]
            val dist = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
            if (dist < minDist) {
                minDist = dist
                nearest = enemy
            }
        }
        return if (minDist < 0.5f) nearest else null
    }

    private fun shouldSpawn(now: Long): Boolean =
        now - lastSpawnTime > SPAWN_INTERVAL_MS

    private fun startNextWave() {
        waveNumber++
        enemiesPerWave += 2
        // Give the player some reward between waves
        ammo += 20
        if (health < 100) health = minOf(100, health + 25)
        Log.i(TAG, "Wave $waveNumber starting — enemies: $enemiesPerWave, health: $health, ammo: $ammo")
    }

    fun getEnemyCount(): Int = enemies.size
    fun getEnemies(): List<Enemy> = enemies

    companion object {
        private const val TAG = "GameEngine"
        private const val MAX_FIRE_RANGE = 15.0f  // meters
        private const val ENEMY_ATTACK_RANGE = 1.5f  // meters
        private const val SPAWN_INTERVAL_MS = 3000L  // 3 seconds between spawns
    }
}

enum class GameState {
    PLAYING,
    PAUSED,
    GAME_OVER,
    VICTORY
}

enum class Weapon(val damage: Int, val fireRateMs: Long) {
    PISTOL(damage = 25, fireRateMs = 300),
    SHOTGUN(damage = 60, fireRateMs = 800),
    CHAINGUN(damage = 20, fireRateMs = 100),
    PLASMA(damage = 80, fireRateMs = 500),
    BFG(damage = 500, fireRateMs = 1500)
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
