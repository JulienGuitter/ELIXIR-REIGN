package com.mjm.elixir_reign.core.tools.sprites.sprite_sheet

data class Frame(
    val index: Int,
    val x: Int,
    val y: Int
)

data class AnimationClip(
    val name: String,
    val startFrame: Int,
    val frameCount: Int,
    val fps: Int,
    val frames: List<Frame>
)

data class SpriteSheet(
    val name: String,
    val cellWidth: Int,
    val cellHeight: Int,
    val columns: Int,
    val clips: List<AnimationClip>
)
