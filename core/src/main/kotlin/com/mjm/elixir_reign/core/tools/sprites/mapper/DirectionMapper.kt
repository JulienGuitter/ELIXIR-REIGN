package com.mjm.elixir_reign.core.tools.sprites.mapper

import com.mjm.elixir_reign.shared.logic.DirectionType

class DirectionMapper {
    fun getDirectionInfo(directionType: DirectionType): Pair<String, Boolean> {
        return when (directionType) {
            DirectionType.UP_LEFT -> Pair("_1", true)
            DirectionType.UP -> Pair("_1", true)
            DirectionType.UP_RIGHT -> Pair("_1", false)
            DirectionType.RIGHT -> Pair("_2", false)
            DirectionType.DOWN_RIGHT -> Pair("_3", false)
            DirectionType.DOWN -> Pair("_3", false)
            DirectionType.DOWN_LEFT -> Pair("_3", true)
            DirectionType.LEFT -> Pair("_2", true)
        }
    }

    fun buildClipName(baseClipName: String, directionType: DirectionType): String {
        val (suffix, _) = getDirectionInfo(directionType)
        return baseClipName + suffix
    }

    fun shouldFlip(directionType: DirectionType): Boolean {
        return getDirectionInfo(directionType).second
    }
}
