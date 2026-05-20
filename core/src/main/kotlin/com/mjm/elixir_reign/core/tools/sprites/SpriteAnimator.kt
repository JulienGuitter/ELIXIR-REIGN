package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.tools.sprites.mapper.ActionMapper
import com.mjm.elixir_reign.core.tools.sprites.mapper.DirectionMapper
import com.mjm.elixir_reign.core.tools.sprites.mapper.StateMapper
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.AnimationClip
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.Frame
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.BuildingState
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * SpriteAnimator gère l'animation d'un sprite
 * Compatible avec:
 * - Units: utilise direction + action pour determiner le clip
 * - Buildings: utilise juste un state simple (pas de direction/action)
 *
 * Pure logique d'animation, pas de dépendances métier
 */
class SpriteAnimator(
    val spriteSheet: SpriteSheet,
    val texturePath: String,
    val baseClipName: String,
    directionType: DirectionType? = null,
    actionType: ActionType? = null,
    buildingState: BuildingState? = null,
    buildingLevel: Int = 1
) {
    private var currentClip: AnimationClip? = null
    private var currentFrameIndex = 0
    private var elapsedTime = 0f
    var currentAction = actionType
        private set
    var currentDirection = directionType
        private set
    var currentBuildingState = buildingState
        private set

    private val actionMapper = ActionMapper()
    private val directionMapper = DirectionMapper()
    private val stateMapper = StateMapper()

    // Flag pour savoir si c'est une unit ou un building
    private val isUnit = directionType != null && actionType != null && buildingState == null

    init {
        // Validation: un bâtiment DOIT avoir un state
        if (!isUnit && buildingState == null) {
            throw IllegalArgumentException("Building sprite must have a buildingState defined!")
        }

        // Initialiser le clip selon le type
        if (isUnit) {
            setUnitClip(baseClipName, directionType!!, actionType!!)
        } else {
            // buildingState ne peut pas être null ici grâce à la validation
            setBuildingClip(baseClipName, buildingState!!, buildingLevel)
        }
    }

    /**
     * Change le clip animation pour une UNIT (avec direction + action)
     * Réinitialise le frame index et elapsed time
     */
    fun setUnitClip(baseClipName: String, directionType: DirectionType, actionType: ActionType) {
        if (!isUnit) throw IllegalStateException("Cannot set direction/action clip for a building sprite")

        currentAction = actionType
        currentDirection = directionType
        val fullClipName = actionMapper.buildClipName(baseClipName, actionType) +
                           directionMapper.getDirectionInfo(directionType).first

        currentClip = spriteSheet.clips.find { it.name == fullClipName }
        currentFrameIndex = 0
        elapsedTime = 0f
    }

    /**
     * Change le clip animation pour un BUILDING (avec state mapping)
     * Utilise StateMapper pour trouver le bon clip basé sur le BuildingState
     */
    fun setBuildingClip(baseClipName: String, buildingState: BuildingState) {
        if (isUnit) throw IllegalStateException("Cannot set building clip for a unit sprite")

        setBuildingClip(baseClipName, buildingState, level = 1)
    }

    fun setBuildingClip(baseClipName: String, buildingState: BuildingState, level: Int) {
        if (isUnit) throw IllegalStateException("Cannot set building clip for a unit sprite")

        // Mettre à jour l'état courant
        currentBuildingState = buildingState

        // Utiliser StateMapper pour construire le nom du clip avec le state
        val requestedLevel = level.coerceAtLeast(1)
        currentClip = findBuildingClip(baseClipName, buildingState, requestedLevel)
        currentFrameIndex = 0
        elapsedTime = 0f
    }

    private fun findBuildingClip(baseClipName: String, buildingState: BuildingState, level: Int): AnimationClip? {
        if (buildingState == BuildingState.MINING) {
            for (candidateLevel in level downTo 1) {
                val clipName = stateMapper.buildClipName(baseClipName, buildingState, candidateLevel)
                val clip = spriteSheet.clips.find { it.name == clipName }
                if (clip != null) {
                    return clip
                }
            }
        }

        val fallbackClipName = stateMapper.buildClipName(baseClipName, buildingState, level)
        return spriteSheet.clips.find { it.name == fallbackClipName }
            ?: spriteSheet.clips.find { it.name == stateMapper.buildClipName(baseClipName, buildingState, 1) }
            ?: spriteSheet.clips.firstOrNull()
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
     * Retourne false pour les bâtiments (pas de direction)
     */
    fun shouldFlip(): Boolean {
        val direction = currentDirection ?: return false
        return directionMapper.shouldFlip(direction)
    }

    /**
     * Récupère le nom du clip actuel
     */
    fun getCurrentClipName(): String? = currentClip?.name

    /**
     * Récupère le collider du clip actuel (null = pas de collider défini)
     */
    fun getCurrentCollider() = currentClip?.collider
}

