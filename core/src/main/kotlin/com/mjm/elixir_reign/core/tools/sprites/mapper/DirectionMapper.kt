package com.mjm.elixir_reign.core.tools.sprites.mapper

import com.mjm.elixir_reign.shared.logic.Direction

class DirectionMapper {
    fun getDirectionInfo(direction: Direction): Pair<String, Boolean> {
        return when (direction) {
            Direction.UP_LEFT -> Pair("_1", true)
            Direction.UP -> Pair("_1", true)
            Direction.UP_RIGHT -> Pair("_1", false)
            Direction.RIGHT -> Pair("_2", false)
            Direction.DOWN_RIGHT -> Pair("_3", false)
            Direction.DOWN -> Pair("_3", false)
            Direction.DOWN_LEFT -> Pair("_3", true)
            Direction.LEFT -> Pair("_2", true)
        }
    }

    fun buildClipName(baseClipName: String, direction: Direction): String {
        val (suffix, _) = getDirectionInfo(direction)
        return baseClipName + suffix
    }

    fun shouldFlip(direction: Direction): Boolean {
        return getDirectionInfo(direction).second
    }
}
