package com.mjm.elixir_reign.android.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mjm.elixir_reign.core.ui.UiAssets

class ShopCard(title: String, price: Int) : Table() {

     init {

         background = UiAssets.skin.getDrawable("shopCardBackground")
         pad(10f)

         val titleLabel = Label(title, UiAssets.skin)
         val priceLabel = Label("$price", UiAssets.skin)
         titleLabel.setAlignment(Align.left)
         priceLabel.setAlignment(Align.left)

         add(titleLabel).expandX().fillX().left()
         row()
         add(priceLabel).expandX().fillX().left()
     }
}
