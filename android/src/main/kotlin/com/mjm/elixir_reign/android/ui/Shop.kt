package com.mjm.elixir_reign.android.ui

import com.mjm.elixir_reign.core.ui.ShopPanel
import com.mjm.elixir_reign.core.ui.ShopVisualConfig

object Shop : ShopPanel(
    cardFactory = { title, price -> ShopCard(title, price) },
    visualConfig = ShopVisualConfig(
        // Keep Android visuals customizable from this module.
        scrollPaneStyle = "shopTransparent"
    )
)
