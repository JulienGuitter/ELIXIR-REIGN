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
    companion object {
        val BARRACKS = BuildingStats(
            name = "Barracks",
            maxHP = 200f,
            costGold = 300,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 5f,
            productionRate = 1f,
            footprintSizeTiles = 3,
            texturePath = "sprites/buildings/dark_elixir_pack.png",
            spriteSheetJsonPath = "",
            spriteBaseClipName = "barracks"
        )
        val ELEXIR_PUMP = BuildingStats(
            name = "Elixir Pump",
            maxHP = 100f,
            costGold = 200,
            costElixir = 0,
            costDarkElixir = 0,
            buildTime = 4f,
            productionRate = 0.5f,
            footprintSizeTiles = 1,
            texturePath = "sprites/buildings/dark_elixir_pack.png",
            spriteSheetJsonPath = "",
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
    }
}
