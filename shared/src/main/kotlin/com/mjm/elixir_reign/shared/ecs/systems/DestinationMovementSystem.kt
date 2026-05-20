package com.mjm.elixir_reign.shared.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.logic.DirectionType
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * DestinationMovementSystem : Déplace une entité vers une destination.
 *
 * Logique :
 * 1. Lit la destination depuis DestinationComponent
 * 2. Calcule l'angle vers la cible
 * 3. Convertit l'angle en DirectionType (8 directions discrètes)
 * 4. Met à jour MovementComponent.directionType et isMoving
 * 5. Le MovementSystem existant fait le déplacement réel
 * 6. Arrête quand la destination est atteinte (distance < threshold)
 */
class DestinationMovementSystem : IteratingSystem(
    Family.all(
        PositionComponent::class.java,
        MovementComponent::class.java,
        DestinationComponent::class.java
    ).get()
) {

    companion object {
        private const val ARRIVAL_THRESHOLD = 5f
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val position = entity.getComponent(PositionComponent::class.java)
        val movement = entity.getComponent(MovementComponent::class.java)
        val destination = entity.getComponent(DestinationComponent::class.java)

        // Si pas de destination active, arrêter le mouvement
        if (!destination.isActive) {
            movement.isMoving = false
            movement.directionX = 0f
            movement.directionY = 0f
            return
        }

        // Calcule la distance vers la cible
        val dx = destination.targetX - position.x
        val dy = destination.targetY - position.y
        val distance = sqrt(dx * dx + dy * dy)

        // Arrivé à destination ?
        if (distance < ARRIVAL_THRESHOLD) {
            destination.isActive = false
            movement.isMoving = false
            movement.directionX = 0f
            movement.directionY = 0f
            position.x = destination.targetX
            position.y = destination.targetY
            return
        }

        // Calcule l'angle vers la destination et convertit en DirectionType
        val angle = atan2(dy, dx)
        movement.directionType = angleToDirection(angle)
        movement.directionX = dx / distance
        movement.directionY = dy / distance
        movement.isMoving = true
    }

    /**
     * Convertit un angle en radians en l'une des 8 directions discrètes.
     *
     * Cercle trigonométrique standard :
     * - 0 rad = 0° = RIGHT (axe X+)
     * - π/2 rad = 90° = UP (axe Y+)
     * - π rad = 180° = LEFT (axe X-)
     * - 3π/2 rad = 270° = DOWN (axe Y-)
     *
     * Les 8 secteurs font π/4 radians (45°) chacun, centrés sur chaque direction.
     */
    private fun angleToDirection(angle: Float): DirectionType {
        // Normalise l'angle entre 0 et 2π
        val normalizedAngle = if (angle < 0) angle + 2 * PI.toFloat() else angle

        // Chaque secteur fait π/4 = 45°
        // On commence à -π/8 pour que RIGHT soit centré à 0
        val sectorAngle = PI.toFloat() / 4f
        val offsetAngle = normalizedAngle + sectorAngle / 2f

        return when ((offsetAngle / sectorAngle).toInt() % 8) {
            0 -> DirectionType.RIGHT
            1 -> DirectionType.UP_RIGHT
            2 -> DirectionType.UP
            3 -> DirectionType.UP_LEFT
            4 -> DirectionType.LEFT
            5 -> DirectionType.DOWN_LEFT
            6 -> DirectionType.DOWN
            7 -> DirectionType.DOWN_RIGHT
            else -> DirectionType.RIGHT
        }
    }
}
