package com.mjm.elixir_reign.shared.data

enum class ResourceType(val displayName: String) {
    ELEXIR("Elexir"),
    GOLD("Or"),
    BLACK_ELEXIR("Elexir noir")
}

data class ResourceCost(
    val resourceType: ResourceType,
    val amount: Int
)
