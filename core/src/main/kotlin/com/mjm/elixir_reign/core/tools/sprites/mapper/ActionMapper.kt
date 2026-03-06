package com.mjm.elixir_reign.core.tools.sprites.mapper

import com.mjm.elixir_reign.shared.logic.ActionType

class ActionMapper {
    fun getActionInfo(actionType: ActionType): String {
        return when (actionType) {
            ActionType.RUN -> "_run"
            ActionType.ATTACK -> "_attack"
        }
    }

    fun buildClipName(baseClipName: String, actionType: ActionType): String {
        val suffix = getActionInfo(actionType)
        return baseClipName + suffix
    }
}
