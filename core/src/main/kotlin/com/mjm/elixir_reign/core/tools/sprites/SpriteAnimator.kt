package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.tools.sprites.mapper.ActionMapper
import com.mjm.elixir_reign.core.tools.sprites.mapper.DirectionMapper
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.AnimationClip
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.Frame
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * SpriteAnimator gère l'animation d'un sprite
 * Pure logique d'animation, pas de dépendances métier
 */
class SpriteAnimator(
    val spriteSheet: SpriteSheet,
    val texturePath: String,
    val baseClipName: String,
    directionType: DirectionType,
    actionType: ActionType
) {
    private var currentClip: AnimationClip? = null
    private var currentFrameIndex = 0
    private var elapsedTime = 0f
    var currentAction = actionType
        private set
    var currentDirection = directionType
        private set

    private val actionMapper = ActionMapper()
    private val directionMapper = DirectionMapper()

    init {
        setClip(baseClipName, directionType, actionType)
    }

    /**
     * Change le clip animation
     * Réinitialise le frame index et elapsed time
     */
    fun setClip(baseClipName: String, directionType: DirectionType, actionType: ActionType) {
        currentAction = actionType
        currentDirection = directionType
        val fullClipName = actionMapper.buildClipName(baseClipName, actionType) +
                           directionMapper.getDirectionInfo(directionType).first

        currentClip = spriteSheet.clips.find { it.name == fullClipName }
        currentFrameIndex = 0
        elapsedTime = 0f
    }

    /**
     * Met à jour l'état d'animation
     * Avance vers le prochain frame si le temps écoulé dépasse la durée d'une frame
     */
    fun update(deltaTime: Float) {
        val clip = currentClip ?: return

        elapsedTime += deltaTime
        val frameDuration = 1f / clip.fps

        if (elapsedTime >= frameDuration) {
            elapsedTime -= frameDuration
            currentFrameIndex = (currentFrameIndex + 1) % clip.frameCount
        }
    }

    /**
     * Récupère le frame actuel
     */
    private fun getCurrentFrame(): Frame? {
        val clip = currentClip ?: return null
        return clip.frames.getOrNull(currentFrameIndex)
    }

    /**
     * Récupère la TextureRegion du frame actuel
     * Crée une nouvelle région à chaque appel
     */
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

    /**
     * Indique si le sprite doit être flippé selon la direction
     */
    fun shouldFlip(): Boolean = directionMapper.shouldFlip(currentDirection)

    /**
     * Récupère le nom du clip actuel
     */
    fun getCurrentClipName(): String? = currentClip?.name
}

