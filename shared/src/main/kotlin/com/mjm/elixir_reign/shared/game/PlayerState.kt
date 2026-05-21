package com.mjm.elixir_reign.shared.game

class PlayerState(
    var id: Int = 0,
    var name: String = "",
    var gold: Int = DEFAULT_GOLD,
    var elixir: Int = DEFAULT_ELIXIR,
    var darkElixir: Int = DEFAULT_DARK_ELIXIR,
    var units: MutableList<UnitState> = mutableListOf(),
    var buildings: MutableList<BuildingInstanceState> = mutableListOf()
) {
    companion object {
        const val DEFAULT_GOLD = 200
        const val DEFAULT_ELIXIR = 150
        const val DEFAULT_DARK_ELIXIR = 0
    }
}
