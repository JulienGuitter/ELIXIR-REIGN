package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent

/**
 * HealthSystem côté client
 * Gère l'affichage visuel de la santé (barres de vie, effets, etc.)
 * La logique critique est gérée côté serveur
 *
 * Components requis:
 * - HealthComponent (santé partagée avec le serveur)
 * - HealthBarComponent (optionnel, pour l'affichage de la barre)
 */
class HealthSystem : IteratingSystem(
    Family.all(HealthComponent::class.java).get()
) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val health = entity.getComponent(HealthComponent::class.java)
        val healthBar = entity.getComponent(HealthBarComponent::class.java)

        // Affichage/animation de la santé côté client
        if (health.currentHP <= 0f) {
            println("[CLIENT] Entity died! Showing death animation...")
            // Ici tu ajouteras les effets visuels, animation de mort, etc.
        }

        // Clamp la santé aux limites
        if (health.currentHP > health.maxHP) {
            health.currentHP = health.maxHP
        }
        if (health.currentHP < 0f) {
            health.currentHP = 0f
        }
    }
}

