package com.mjm.elixir_reign.shared.factory

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Engine
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent

object EntityFactory {

    fun createBarbarian(x: Float, y: Float, engine: Engine): Entity {
        val barbarian = Entity()
        barbarian.add(PositionComponent(x, y))
        barbarian.add(HealthComponent(currentHP = 100f, maxHP = 100f))
        barbarian.add(SpriteComponent("barbarian.png", width = 32, height = 32))

        // Ajouter l'entité à l'engine
        engine.addEntity(barbarian)

        return barbarian
    }
}

