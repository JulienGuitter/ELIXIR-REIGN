package com.mjm.elixir_reign.core.session

import com.mjm.elixir_reign.shared.type.GameType

object GameSession {
    @Volatile
    var mode: GameMode = GameMode.SOLO

    @Volatile
    var gameType: GameType = GameType.SOLO

    @Volatile
    var gold: Int = DEFAULT_GOLD

    @Volatile
    var elixir: Int = DEFAULT_ELIXIR

    @Volatile
    var darkElixir: Int = DEFAULT_DARK_ELIXIR

    fun startSolo() {
        mode = GameMode.SOLO
        gameType = GameType.SOLO
        resetResources()
    }

    fun startMultiplayer(gameType: GameType) {
        mode = GameMode.MULTI
        this.gameType = gameType
        resetResources()
    }

    private fun resetResources() {
        gold = DEFAULT_GOLD
        elixir = DEFAULT_ELIXIR
        darkElixir = DEFAULT_DARK_ELIXIR
    }

    private const val DEFAULT_GOLD = 1200
    private const val DEFAULT_ELIXIR = 1200
    private const val DEFAULT_DARK_ELIXIR = 80
}
