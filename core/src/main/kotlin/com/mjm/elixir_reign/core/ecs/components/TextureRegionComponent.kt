package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Component qui stocke la TextureRegion à afficher
 * Créée UNE FOIS à la création de l'entité
 * Réutilisée par RenderSystem pour le rendu
 */
class TextureRegionComponent(
    var textureRegion: TextureRegion
) : Component

