package com.mjm.elixir_reign.shared.data

/**
 * Classe mère abstraite pour toutes les entités (Units et Buildings)
 * Contient UNIQUEMENT les propriétés communes à tous
 * Chaque classe fille peut avoir ses propres propriétés spécifiques
 */
abstract class EntityStats(
    open var name: String,
    open var maxHP: Float,
    open var texturePath: String,
    open var costGold: Int,
    open var costElixir: Int,
    open var costDarkElixir: Int,
    open var spriteSheetJsonPath: String,
    open var spriteBaseClipName: String,
)
