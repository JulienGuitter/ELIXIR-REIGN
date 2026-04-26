package com.mjm.elixir_reign.shared.game

class PlayerState(
    var id: Int = 0,
    var name: String = "",
    var gold: Int = DEFAULT_GOLD,
    var elixir: Int = DEFAULT_ELIXIR,
    var darkElixir: Int = DEFAULT_DARK_ELIXIR,
    var units: MutableList<UnitState> = mutableListOf()
) {
    companion object {
        const val DEFAULT_GOLD = 1200
        const val DEFAULT_ELIXIR = 1200
        const val DEFAULT_DARK_ELIXIR = 80
    }
}
