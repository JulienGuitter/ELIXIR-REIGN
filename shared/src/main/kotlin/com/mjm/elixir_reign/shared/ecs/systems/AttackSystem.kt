package com.mjm.elixir_reign.shared.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.shared.ecs.components.AttackComponent

class AttackSystem : IteratingSystem(
    Family.all(AttackComponent::class.java).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val attack = entity.getComponent(AttackComponent::class.java)

        // Réduire le cooldown d'attaque
        if (attack.attackCooldown > 0f) {
            attack.attackCooldown -= deltaTime
        }
    }
}
