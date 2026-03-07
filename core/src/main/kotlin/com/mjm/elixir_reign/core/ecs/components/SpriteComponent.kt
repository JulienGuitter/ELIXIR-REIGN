package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.ColliderData

class SpriteComponent(
    var texturePath: String = "",
    var width: Int = 32,
    var height: Int = 32,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var offsetX: Float = 0f,       // Décalage horizontal dû au padding de l'animation
    var offsetY: Float = 0f,       // Décalage vertical dû au padding de l'animation
    var collider: ColliderData? = null  // Collider normalisé du clip actif (null = toute la cellule)
) : Component
