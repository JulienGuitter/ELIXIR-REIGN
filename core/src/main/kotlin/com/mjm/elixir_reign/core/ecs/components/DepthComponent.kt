package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * DepthComponent : Gère l'ordre d'affichage (z-order) d'une entité
 *
 * Deux modes de tri :
 * 1. Y-sorting automatique (défaut) : Utilisé quand zOrder est null
 *    → Plus Y = plus bas dans la hiérarchie isométrique = affiche en dernier
 * 2. Z-order explicite : Quand zOrder est non-null
 *    → Ignore le Y-sorting et utilise cette valeur fixe pour l'ordre d'affichage
 *
 * @param zOrder Profondeur explicite (optionnel).
 *               Si null, RenderSystem utilisera automatiquement yPosition pour le tri.
 *               Si non-null, cette valeur remplace complètement le Y-sorting.
 */
class DepthComponent(
    var zOrder: Float? = null  // Si null, RenderSystem utilise Y-sorting automatique
) : Component {

    /**
     * Calcule la profondeur finale à utiliser pour l'affichage
     * @param yPosition Position Y de l'entité (utilisée si zOrder est null)
     * @return La profondeur à utiliser pour le tri (zOrder si fourni, sinon yPosition)
     */
    fun getDepth(yPosition: Float): Float {
        return zOrder ?: yPosition
    }
}
