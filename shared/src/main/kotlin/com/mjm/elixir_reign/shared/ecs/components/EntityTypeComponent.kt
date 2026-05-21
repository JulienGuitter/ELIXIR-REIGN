package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.EntityType

/**
 * EntityTypeComponent : Représente le type d'une entité (unité ou bâtiment)
 *
 * Unifie le typage des unités et des bâtiments
 * Permet aux systèmes de retrouver facilement le type d'une entité
 *
 * @param entityType Le type de l'entité (BARBARIAN, ARCHER, GIANT, BARRACKS, ELEXIR_PUMP, DARCKELEXIR_PUMP)
 */
class EntityTypeComponent(
    var entityType: EntityType
) : Component
