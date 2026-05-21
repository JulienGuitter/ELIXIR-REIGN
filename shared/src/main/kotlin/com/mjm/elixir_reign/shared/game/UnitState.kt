package com.mjm.elixir_reign.shared.game

import com.mjm.elixir_reign.shared.logic.EntityType

class UnitState(
    var id: Int = 0,
    var ownerPlayerId: Int = 0,
    var entityType: EntityType = EntityType.BARBARIAN,
    var row: Float = 0f,
    var col: Float = 0f,
    var targetRow: Float = 0f,
    var targetCol: Float = 0f,
    var moving: Boolean = false,
    var currentHP: Float = 100f,
    var maxHP: Float = 100f,
    var attackCooldown: Float = 0f,
    var targetUnitId: Int = 0,
    var targetBuildingId: Int = 0,
    var barracksId: Int = 0
)
