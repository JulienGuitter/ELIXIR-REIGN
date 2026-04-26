package com.mjm.elixir_reign.core.session

import com.mjm.elixir_reign.shared.type.GameType

object GameSession {
    @Volatile
    var mode: GameMode = GameMode.SOLO

    @Volatile
    var gameType: GameType = GameType.SOLO

    fun startSolo() {
        mode = GameMode.SOLO
        gameType = GameType.SOLO
    }

    fun startMultiplayer(gameType: GameType) {
        mode = GameMode.MULTI
        this.gameType = gameType
    }
}

