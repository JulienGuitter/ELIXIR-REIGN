package com.mjm.elixir_reign.shared.game

import com.mjm.elixir_reign.shared.logic.EntityType

class BuildingInstanceState(
    var id: Int = 0,
    var ownerPlayerId: Int = 0,
    var entityType: EntityType = EntityType.GOLD_MINE,
    var row: Int = 0,
    var col: Int = 0,
    var level: Int = 1,
    var productionAccumulator: Float = 0f
)
