package com.mjm.elixir_reign.shared.data

import com.mjm.elixir_reign.shared.logic.EntityType

data class BuildingDefinition(
    val entityType: EntityType,
    val stats: BuildingStats
)

object BuildingCatalog {
    val ALL = listOf(
        BuildingDefinition(EntityType.BARRACKS, BuildingStats.BARRACKS),
        BuildingDefinition(EntityType.ELEXIR_PUMP, BuildingStats.ELEXIR_PUMP),
        BuildingDefinition(EntityType.GOLD_MINE, BuildingStats.GOLD_MINE),
        BuildingDefinition(EntityType.DARCKELEXIR_PUMP, BuildingStats.DARCKELEXIR_PUMP),
        BuildingDefinition(EntityType.ARCHER_TOWER, BuildingStats.ARCHER_TOWER),
    )
}

