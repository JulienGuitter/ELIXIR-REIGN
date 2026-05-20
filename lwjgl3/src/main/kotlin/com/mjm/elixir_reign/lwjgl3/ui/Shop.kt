package com.mjm.elixir_reign.lwjgl3.ui

import com.mjm.elixir_reign.core.ui.ShopPanel
import com.mjm.elixir_reign.core.ui.ShopVisualConfig

object Shop : ShopPanel(
    cardFactory = { title, price, preview -> ShopCard(title, price, preview) },
    visualConfig = ShopVisualConfig(
        scrollPaneStyle = "shopTransparent"
    )
)
