package com.mjm.elixir_reign.android.ui

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.shared.data.ResourceCost
import com.mjm.elixir_reign.shared.data.ResourceType

class ShopCard(title: String, costs: List<ResourceCost>, preview: Drawable?) : Table() {

     init {

         background = UiAssets.skin.getDrawable("shopCardBackground")
         pad(10f)

         if (preview != null) {
             val image = Image(preview)
             add(image).size(96f).padBottom(6f)
             row()
         }

         val titleLabel = Label(title, UiAssets.skin)
         titleLabel.setAlignment(Align.left)

         add(titleLabel).expandX().fillX().left()
         row()
         add(costsTable(costs)).expandX().fillX().left()
     }

     private fun costsTable(costs: List<ResourceCost>): Table {
         return Table().apply {
             if (costs.isEmpty()) {
                 add(Label("0", UiAssets.skin)).left()
                 return@apply
             }

             costs.forEach { cost ->
                 val amountLabel = Label("${cost.amount}", UiAssets.skin)
                 amountLabel.setAlignment(Align.left)
                 add(amountLabel).left().padRight(4f)
                 add(Image(UiAssets.texture(iconFor(cost.resourceType)))).size(24f).left().padRight(8f)
             }
         }
     }

     private fun iconFor(resourceType: ResourceType): UiImage {
         return when (resourceType) {
             ResourceType.GOLD -> UiImage.ICON_GOLD
             ResourceType.ELEXIR -> UiImage.ICON_ELIXIR
             ResourceType.BLACK_ELEXIR -> UiImage.ICON_DARK_ELIXIR
         }
     }
}
