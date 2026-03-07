package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * Component côté client pour gérer l'affichage de la barre de vie
 * La barre s'affiche seulement quand HP < maxHP
 * La position et la largeur sont calculées dynamiquement depuis le collider du SpriteComponent
 */
class HealthBarComponent(
    var barHeight: Float = 5f
) : Component


