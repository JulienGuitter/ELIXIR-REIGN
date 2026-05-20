package com.mjm.elixir_reign.shared.data

data class UnitStats(
    override var name: String,
    override var maxHP: Float,
    override var texturePath: String,
    override var costGold: Int,
    override var costElixir: Int,
    override var costDarkElixir: Int,
    override var spriteSheetJsonPath: String,
    override var spriteBaseClipName: String,
    val damage: Float,
    val attackSpeed: Float,
    val range: Float,
    val speed: Float,
    val costs: List<ResourceCost>,
    val trainingTimeSeconds: Float
) : EntityStats(name, maxHP, texturePath, costGold, costElixir, costDarkElixir, spriteSheetJsonPath, spriteBaseClipName) {
    companion object {
        val BARBARIAN = UnitStats(
            name = "Barbarian",
            maxHP = 100f,
            damage = 15f,
            attackSpeed = 1.2f,
            range = 30f,
            speed = 60f,
            costs = listOf(ResourceCost(ResourceType.ELEXIR, 100)),
            trainingTimeSeconds = 4f,
            costGold = 0,
            costElixir = 100,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_barbarian.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_barbarian.json",
            spriteBaseClipName = "barbarian"
        )

        val ARCHER = UnitStats(
            name = "Archer",
            maxHP = 50f,
            damage = 20f,
            attackSpeed = 1.5f,
            range = 100f,
            speed = 70f,
            costs = listOf(
                ResourceCost(ResourceType.ELEXIR, 120),
                ResourceCost(ResourceType.GOLD, 20)
            ),
            trainingTimeSeconds = 5f,
            costGold = 20,
            costElixir = 120,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_archer.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_archer.json",
            spriteBaseClipName = "archer"
        )

        val GIANT = UnitStats(
            name = "Giant",
            maxHP = 150f,
            damage = 10f,
            attackSpeed = 0.5f,
            range = 30f,
            speed = 20f,
            costs = listOf(
                ResourceCost(ResourceType.ELEXIR, 250),
                ResourceCost(ResourceType.GOLD, 80)
            ),
            trainingTimeSeconds = 10f,
            costGold = 80,
            costElixir = 250,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_giant.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_giant.json",
            spriteBaseClipName = "giant"
        )
    }
}
