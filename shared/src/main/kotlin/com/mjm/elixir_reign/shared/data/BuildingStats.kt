package com.mjm.elixir_reign.shared.data
/**
 * Stats spécifiques aux bâtiments
 * Les propriétés communes (name, maxHP, texturePath, costs) viennent d'EntityStats
 * Les propriétés spécifiques aux bâtiments (width, height, buildTime) sont ici
 */
data class BuildingStats(
    override var name: String,
    override var maxHP: Float,
    override var texturePath: String,
    override var costGold: Int,
    override var costElixir: Int,
    override var costDarkElixir: Int,
    // Métadonnées de sprite
    override var spriteSheetJsonPath: String,
    override var spriteBaseClipName: String,
    // Propriétés spécifiques aux bâtiments
    val buildTime: Float,
    val productionRate: Float = 0f,
    // Empreinte de construction en tuiles (NxN)
    val footprintSizeTiles: Int = 1
) : EntityStats(name, maxHP, texturePath, costGold, costElixir, costDarkElixir, spriteSheetJsonPath, spriteBaseClipName) {
    fun primaryCost(): Int {
        return listOf(costGold, costElixir, costDarkElixir).firstOrNull { it > 0 } ?: 0
    }

    companion object {
        val BARRACKS = BuildingStats(
            name = "Barracks",
            maxHP = 220f,
            costGold = 300,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 6f,
            productionRate = 0f,
            footprintSizeTiles = 3,
            texturePath = "sprites/buildings/tileset_troops_factory.png",
            spriteSheetJsonPath = "sprites/buildings/tileset_troops_factory.json",
            spriteBaseClipName = "troops_factory"
        )
        val ELEXIR_PUMP = BuildingStats(
            name = "Elixir Pump",
            maxHP = 100f,
            costGold = 200,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 4f,
            productionRate = 0.5f,
            footprintSizeTiles = 2,
            texturePath = "sprites/buildings/anim_pack_elixir.png",
            spriteSheetJsonPath = "sprites/buildings/anim_pack_elixir.json",
            spriteBaseClipName = "elixir_pump"
        )
        val DARCKELEXIR_PUMP = BuildingStats(
            name = "Dark Elixir Pump",
            maxHP = 120f,
            costGold = 250,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 4f,
            productionRate = 0.3f,
            footprintSizeTiles = 3,
            texturePath = "sprites/buildings/dark_elixir_pack.png",
            spriteSheetJsonPath = "sprites/buildings/dark_elixir_pack.json",
            spriteBaseClipName = "darkelixir_pump"
        )
        val GOLD_MINE = BuildingStats(
            name = "Gold Mine",
            maxHP = 110f,
            costGold = 0,
            costElixir = 150,
            costDarkElixir = 0,
            buildTime = 4f,
            productionRate = 0.5f,
            footprintSizeTiles = 2,
            texturePath = "sprites/buildings/anim_pack_mine.png",
            spriteSheetJsonPath = "sprites/buildings/anim_pack_mine.json",
            spriteBaseClipName = "gold_mine"
        )
        val ARCHER_TOWER = BuildingStats(
            name = "Archer Tower",
            maxHP = 180f,
            costGold = 250,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 6f,
            productionRate = 0f,
            footprintSizeTiles = 3,
            texturePath = "sprites/buildings/tileset_archer_tower.png",
            spriteSheetJsonPath = "sprites/buildings/tileset_archer_tower.json",
            spriteBaseClipName = "archer_tower"
        )
        val TOWN_HALL = BuildingStats(
            name = "Town Hall",
            maxHP = 400f,
            costGold = 500,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 10f,
            productionRate = 0f,
            footprintSizeTiles = 4,
            texturePath = "sprites/buildings/tileset_hdv.png",
            spriteSheetJsonPath = "sprites/buildings/tileset_hdv.json",
            spriteBaseClipName = "hdv"
        )
    }
}
