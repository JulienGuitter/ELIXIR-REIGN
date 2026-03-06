package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Component qui indique le type d'unité
 * Utilisé par AnimationSystem pour savoir quel baseClipName utiliser
 */
class UnitTypeComponent(
    var unitType: UnitType
) : Component

