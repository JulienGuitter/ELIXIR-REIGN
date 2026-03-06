package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.tools.sprites.mapper.ActionMapper
import com.mjm.elixir_reign.core.tools.sprites.mapper.DirectionMapper
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.AnimationClip
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.Frame
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.shared.logic.Action
import com.mjm.elixir_reign.shared.logic.Direction

class SpriteAnimator(
    val spriteSheet: SpriteSheet,
    val texturePath: String,
    baseClipName: String,
    direction: Direction,
    action: Action
) {
    private var currentClip: AnimationClip? = null
    private var currentFrameIndex = 0
    private var elapsedTime = 0f
    private var currentAction = action
    private val actionMapper = ActionMapper()
    private var currentDirection = direction
    private val directionMapper = DirectionMapper()

    init {
        setClip(baseClipName, direction, action)
    }

    fun setClip(baseClipName: String, direction: Direction, action: Action) {
        currentAction = action
        currentDirection = direction
        val fullClipName = actionMapper.buildClipName(baseClipName, action) +
                           directionMapper.buildClipName(baseClipName, direction)

        currentClip = spriteSheet.clips.find { it.name == fullClipName }
        currentFrameIndex = 0
        elapsedTime = 0f
    }

    fun update(deltaTime: Float) {
        val clip = currentClip ?: return

        elapsedTime += deltaTime
        val frameDuration = 1f / clip.fps

        if (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrameIndex = (currentFrameIndex + 1) % clip.frameCount
        }
    }

    private fun getCurrentFrame(): Frame? {
        val clip = currentClip ?: return null
        return clip.frames.getOrNull(currentFrameIndex)
    }

    fun getCurrentTextureRegion(): TextureRegion? {
        val frame = getCurrentFrame() ?: return null
        val texture = TextureManager.getTexture(texturePath)

        val region = TextureRegion(
            texture,
            frame.x,
            frame.y,
            spriteSheet.cellWidth,
            spriteSheet.cellHeight
        )

        if (shouldFlip()) {
            region.flip(true, false)
        }

        return region
    }

    fun shouldFlip(): Boolean = directionMapper.shouldFlip(currentDirection)

    fun getCurrentClipName(): String? = currentClip?.name
}
