package com.mjm.elixir_reign.shared.data

data class UnitStats(
    override var name: String,
    override var maxHP: Float,
    override var texturePath: String,
    override var costGold: Int,
    override var costElixir: Int,
    override var costDarkElixir: Int,
    // Métadonnées de sprite
    override var spriteSheetJsonPath: String,
    override var spriteBaseClipName: String,
    // Propriétés spécifiques aux unités
    val damage: Float,
    val attackSpeed: Float,
    val range: Float,
    val speed: Float,
    val trainingTimeSeconds: Float,
    // Métadonnées de sprite
) : EntityStats(name, maxHP, texturePath, costGold, costElixir, costDarkElixir, spriteSheetJsonPath, spriteBaseClipName) {
    companion object {
        val BARBARIAN = UnitStats(
            name = "Barbarian",
            maxHP = 100f,
            damage = 15f,
            attackSpeed = 1.2f,
            range = 2.5f,
            speed = 60f,
            costGold = 100,
            costElixir = 0,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_barbarian.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_barbarian.json",
            spriteBaseClipName = "barbarian",
            trainingTimeSeconds = 2.5f
        )

        val ARCHER = UnitStats(
            name = "Archer",
            maxHP = 50f,
            damage = 20f,
            attackSpeed = 1.5f,
            range = 4f,
            speed = 70f,
            costGold = 150,
            costElixir = 0,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_archer.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_archer.json",
            spriteBaseClipName = "archer",
            trainingTimeSeconds = 2.5f
        )

        val GIANT = UnitStats(
            name = "Giant",
            maxHP = 150f,
            damage = 10f,
            attackSpeed = 0.5f,
            range = 2.5f,
            speed = 20f,
            costGold = 150,
            costElixir = 0,
            costDarkElixir = 0,
            texturePath = "sprites/units/anim_pack_chr_giant.png",
            spriteSheetJsonPath = "sprites/units/anim_pack_chr_giant.json",
            spriteBaseClipName = "giant",
            trainingTimeSeconds = 6f
        )
    }
}
