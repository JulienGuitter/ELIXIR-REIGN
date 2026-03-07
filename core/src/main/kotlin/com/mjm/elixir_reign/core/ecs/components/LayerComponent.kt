package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

/**
 * LayerComponent : Gère les couches de rendu (z-layers)
 * Permet de contrôler précisément quel élément s'affiche au-dessus d'un autre
 *
 * @param layer Numéro de couche (plus élevé = au-dessus)
 *     - 0 : Décors de fond
 *     - 1-10 : Entités principales (triées par Y-sorting)
 *     - 100+ : Effets, UI, sélection
 *     - 9999 : Interface utilisateur globale
 */
class LayerComponent(
    var layer: Int = 1
) : Component
