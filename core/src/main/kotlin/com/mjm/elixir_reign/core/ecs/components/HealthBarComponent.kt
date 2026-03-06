package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * Component côté client pour gérer l'affichage de la barre de vie
 * La barre s'affiche seulement quand HP < maxHP
 */
class HealthBarComponent(
    var barWidth: Float = 30f,
    var barHeight: Float = 4f,
    var offsetX: Float = 0f,
    var offsetY: Float = -15f
) : Component


