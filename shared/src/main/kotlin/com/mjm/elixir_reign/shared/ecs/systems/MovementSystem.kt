package com.mjm.elixir_reign.shared.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.logic.DirectionVectors

/**
 * MovementSystem partagé (client + serveur)
 * Gère le mouvement des entités selon leur direction et vitesse
 * Optimisé pour les 8 directions discrètes
 */
class MovementSystem : IteratingSystem(
    Family.all(PositionComponent::class.java, MovementComponent::class.java).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val position = entity.getComponent(PositionComponent::class.java)
        val movement = entity.getComponent(MovementComponent::class.java)

        // Ne bouger que si l'entité est en mouvement
        if (movement.isMoving) {
            val (vx, vy) = if (movement.velocityX != 0f || movement.velocityY != 0f) {
                Pair(movement.velocityX, movement.velocityY)
            } else {
                DirectionVectors.getVector(movement.directionType)
            }

            // Mettre à jour la position
            position.x += vx * movement.speed * deltaTime
            position.y += vy * movement.speed * deltaTime
        }
    }
}
