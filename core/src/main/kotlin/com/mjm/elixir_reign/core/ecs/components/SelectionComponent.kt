package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * Component côté client pour gérer la sélection d'une entité
 * Stocke l'état de sélection et les paramètres visuels du contour
 *
 * Ce component est indépendant des systems de rendu et d'animation
 * Il permet à SelectionRenderSystem de savoir comment afficher le contour
 */
class SelectionComponent(
    var isSelected: Boolean = false,
    var XOffset: Float = 0f,
    var YOffset: Float = 0f
) : Component

