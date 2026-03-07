package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent

/**
 * DamageTestSystem - Système de test pour inflige des dégâts aux entités
 * UNIQUEMENT À DES FINS DE TEST - À SUPPRIMER EN PRODUCTION
 *
 * Ce système réduit automatiquement la santé des entités pour tester
 * le rendu des barres de vie et les effets de fade-out
 */
class DamageTestSystem : IteratingSystem(
    Family.all(
        HealthComponent::class.java,
        HealthBarComponent::class.java
    ).get()
) {
    private var damageTimer = 0f
    private var firstEntity = true

    override fun processEntity(entity: Entity, deltaTime: Float) {
        // Infliger des dégâts à la première entité tous les 0.5 secondes
        if (firstEntity) {
            damageTimer += deltaTime
            if (damageTimer >= 0.5f) {
                val health = entity.getComponent(HealthComponent::class.java)
                if (health.currentHP > 0f) {
                    health.currentHP -= 10f
                    println("[TEST] Dégâts infligés! HP: ${health.currentHP} / ${health.maxHP}")
                }
                damageTimer = 0f
            }
            firstEntity = false
        } else {
            firstEntity = true
        }
    }
}

