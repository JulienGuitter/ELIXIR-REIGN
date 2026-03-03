package com.mjm.elixir_reign.core.tools.sprites.mapper

import com.mjm.elixir_reign.shared.logic.Action

class ActionMapper {
    fun getActionInfo(action: Action): String {
        return when (action) {
            Action.RUN -> "_run"
            Action.ATTACK -> "_attack"
        }
    }

    fun buildClipName(baseClipName: String, action: Action): String {
        val suffix = getActionInfo(action)
        return baseClipName + suffix
    }
}
