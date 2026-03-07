package com.mjm.elixir_reign.core.tools.sprites.sprite_sheet

data class Frame(
    val index: Int,
    val x: Int,
    val y: Int
)

data class ColliderData(
    val bottomLeftX: Float,
    val bottomLeftY: Float,
    val topRightX: Float,
    val topRightY: Float
)

data class AnimationClip(
    val name: String,
    val startFrame: Int,
    val frameCount: Int,
    val fps: Int,
    val collider: ColliderData? = null,
    val frames: List<Frame>
)

data class SpriteSheet(
    val name: String,
    val cellWidth: Int,
    val cellHeight: Int,
    val columns: Int,
    val footX: Float,
    val footY: Float,
    val clips: List<AnimationClip>
)
