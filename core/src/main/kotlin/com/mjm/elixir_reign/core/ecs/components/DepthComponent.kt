package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * DepthComponent : Gère l'ordre d'affichage (z-order) d'une entité
 *
 * Deux modes :
 * 1. Y-sorting (défaut, automatique) : Plus Y = plus bas = affiche en dernier
 * 2. Z-order explicite : Une valeur fixe qui remplace le Y-sorting
 *
 * @param zOrder Profondeur explicite (optionnel). Si non-null, ignore le Y-sorting
 * @param useYSorting Si true, calcule la profondeur en fonction de Y (par défaut true)
 */
class DepthComponent(
    var zOrder: Float? = null,  // Si null, utilise Y-sorting
    var useYSorting: Boolean = true
) : Component {

    /**
     * Calcule la profondeur finale à utiliser pour l'affichage
     * @param yPosition Position Y de l'entité (pour Y-sorting)
     * @return La profondeur à utiliser
     */
    fun getDepth(yPosition: Float): Float {
        return zOrder ?: yPosition
    }
}
