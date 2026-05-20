package com.mjm.elixir_reign.shared.data

data class UnitStats(
    val name: String,
    val maxHP: Float,
    val damage: Float,
    val attackSpeed: Float,
    val range: Float,
    val speed: Float,
    val costs: List<ResourceCost>,
    val trainingTimeSeconds: Float,
    val texturePath: String
) {
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
            texturePath = "barbarian.png"
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
            texturePath = "archer.png"
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
            texturePath = "giant.png"
        )
    }
}
