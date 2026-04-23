package com.mjm.elixir_reign.core.ui

enum class UiImage(
    val path: String,
    val minimal: Boolean = false,
    val linearFilter: Boolean = false,
    val loadedByAssetManager: Boolean = true
) {
    BACKGROUND("ui/background.png", minimal = true, loadedByAssetManager = false),
    LOGO_TRANSPARENT("ui/icon_transp.png", minimal = true),
    BUTTON_9PATCH("ui/btn_9patch.png"),
    LEFT_PANEL_9PATCH("ui/left_panel_9patch.png"),
    SHOP_CARD_9PATCH("ui/shop_card_9patch.png"),
    ICON_HAMMER("icons/hammer.png"),
    ICON_SELECT("icons/selection.png"),
    ICON_CLOSE("icons/close.png")
}
