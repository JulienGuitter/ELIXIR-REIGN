package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

/**
 * SelectableComponent : Marque une entité comme sélectionnable et stocke son état de sélection
 *
 * Source unique de vérité pour l'état de sélection d'une entité.
 * Utilisé par :
 * - SelectionInputHandler (client) : détecte les clics et met à jour isSelected
 * - SelectionRenderSystem (client) : affiche les contours de sélection
 */
class SelectableComponent(
    var isSelected: Boolean = false
) : Component

