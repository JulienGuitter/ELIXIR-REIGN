package com.mjm.elixir_reign.shared.data

data class BuildingStats(
    val name: String,
    val maxHP: Float,
    val width: Int,
    val height: Int,
    val cost: Int,
    val buildTime: Float,
    val texturePath: String,
    val productionRate: Float = 0f,
    val maxFormedTroops: Int = 0
) {
    companion object {
        val BARRACKS = BuildingStats(
            name = "Barracks",
            maxHP = 200f,
            width = 64,
            height = 64,
            cost = 300,
            buildTime = 5f,
            texturePath = "barracks.png",
            productionRate = 1f,
            maxFormedTroops = 6
        )

        val TOWER = BuildingStats(
            name = "Tower",
            maxHP = 150f,
            width = 48,
            height = 48,
            cost = 250,
            buildTime = 3f,
            texturePath = "tower.png"
        )

        val RESOURCE_BUILDING = BuildingStats(
            name = "Resource Building",
            maxHP = 100f,
            width = 64,
            height = 64,
            cost = 200,
            buildTime = 4f,
            texturePath = "resource_building.png",
            productionRate = 0.5f
        )
    }
}
