package com.ardoom.game

/**
 * Pickup items that spawn in the AR world when enemies are defeated or waves progress.
 */
enum class PickupType(
    val healthBonus: Int,
    val ammoBonus: Int,
    val armorBonus: Int,
    val pointValue: Int,
    val color: FloatArray, // RGB float array [0..1]
    val iconSize: Float    // Scale in AR space
) {
    HEALTH(
        healthBonus = 25,
        ammoBonus = 0,
        armorBonus = 0,
        pointValue = 50,
        color = floatArrayOf(0.1f, 0.9f, 0.2f), // Bright green
        iconSize = 0.35f
    ),
    AMMO(
        healthBonus = 0,
        ammoBonus = 20,
        armorBonus = 0,
        pointValue = 30,
        color = floatArrayOf(0.9f, 0.8f, 0.1f), // Bright yellow
        iconSize = 0.3f
    ),
    ARMOR(
        healthBonus = 0,
        ammoBonus = 0,
        armorBonus = 25,
        pointValue = 50,
        color = floatArrayOf(0.2f, 0.5f, 0.9f), // Blue
        iconSize = 0.35f
    )
}

data class Pickup(
    val type: PickupType,
    var position: FloatArray,
    var isCollected: Boolean = false,
    var rotation: Float = 0f
) {
    private var floatTime: Float = (Math.random() * Math.PI * 2).toFloat()
    private val basePosition: FloatArray = position.clone()

    fun update(deltaTime: Float) {
        if (isCollected) return

        // Spin around Y axis
        rotation = (rotation + deltaTime * 120f) % 360f

        // Gentle floating up and down
        floatTime += deltaTime * 2.5f
        if (position.size >= 3 && basePosition.size >= 3) {
            position[1] = basePosition[1] + Math.sin(floatTime.toDouble()).toFloat() * 0.08f
        }
    }

    fun shouldCollect(cameraPos: FloatArray): Boolean {
        if (isCollected) return false
        val dx = position[0] - cameraPos[0]
        val dy = position[1] - cameraPos[1]
        val dz = position[2] - cameraPos[2]
        val distance = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        return distance < COLLECTION_RADIUS
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pickup) return false
        if (type != other.type) return false
        if (!position.contentEquals(other.position)) return false
        if (isCollected != other.isCollected) return false
        if (rotation != other.rotation) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + position.contentHashCode()
        result = 31 * result + isCollected.hashCode()
        result = 31 * result + rotation.hashCode()
        return result
    }

    companion object {
        const val COLLECTION_RADIUS = 1.0f
    }
}
