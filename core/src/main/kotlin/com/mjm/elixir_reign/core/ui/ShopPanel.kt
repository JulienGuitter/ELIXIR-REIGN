package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.mjm.elixir_reign.core.i18n.Localization

data class ShopVisualConfig(
    val backgroundDrawable: String = "shopBackground",
    val closeButtonStyle: String = "shopCloseButton",
    val scrollPaneStyle: String = "shopTransparent",
    val containerPadding: Float = 20f,
    val closeButtonSize: Float = 30f,
    val closeButtonIconPadding: Float = 5f,
    val cardsSpacing: Float = 12f,
    val cardsRowHeight: Float = 220f,
    val scrollAmount: Float = 75f
)

open class ShopPanel(
    private val cardFactory: (title: String, price: Int) -> Actor,
    private val visualConfig: ShopVisualConfig = ShopVisualConfig()
) : Table() {

    private val itemsTable = Table()
    private val container = Table()
    private lateinit var scrollPane: ScrollPane

    init {
        touchable = Touchable.enabled

        setFillParent(true)
        align(Align.bottom)
        isVisible = false

        container.background = UiAssets.skin.getDrawable(visualConfig.backgroundDrawable)
        container.pad(visualConfig.containerPadding)

        val title = Label(Localization.get("shop.title"), UiAssets.skin)
        title.setAlignment(Align.left)

        val closeBtn = ImageButton(UiAssets.skin.get(visualConfig.closeButtonStyle, ImageButton.ImageButtonStyle::class.java))
        closeBtn.imageCell.pad(visualConfig.closeButtonIconPadding)
        closeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                hide()
            }
        })

        val topTable = Table()
        topTable.add(title).expandX().fillX().left().padBottom(10f)
        topTable.add(closeBtn).size(visualConfig.closeButtonSize).padBottom(10f)

        itemsTable.defaults().padRight(visualConfig.cardsSpacing)
        for (i in 1..20) {
            itemsTable.add(cardFactory("Item $i", i * 10))
        }

        scrollPane = ScrollPane(itemsTable, UiAssets.skin, visualConfig.scrollPaneStyle)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(false, true)
        scrollPane.setForceScroll(true, false)
        scrollPane.setScrollbarsOnTop(true)
        scrollPane.addListener(object : InputListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (!isVisible || pointer != -1) {
                    return
                }
                stage?.setScrollFocus(scrollPane)
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                if (!isVisible || pointer != -1) {
                    return
                }

                if (toActor == null || !toActor.isDescendantOf(scrollPane)) {
                    if (stage?.scrollFocus === scrollPane) {
                        stage?.setScrollFocus(null)
                    }
                }
            }

            override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
                if (!isVisible) {
                    return false
                }

                val wheelAmount = (amountY + amountX) * visualConfig.scrollAmount
                scrollPane.scrollX += wheelAmount
                scrollPane.updateVisualScroll()
                return true
            }
        })

        container.add(topTable).expandX().fillX().row()
        container.add(scrollPane).expandX().fillX().height(visualConfig.cardsRowHeight)

        add(container).expand().fillX().bottom()

        addCaptureListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (!isVisible) {
                    return false
                }

                val insideContainer = x in container.x..(container.x + container.width) &&
                    y in container.y..(container.y + container.height)

                if (!insideContainer) {
                    stage?.setScrollFocus(null)
                    stage?.setKeyboardFocus(null)
                    stage?.cancelTouchFocus()
                    return false
                }

                return false
            }
        })
    }

    fun show() {
        isVisible = true
        toFront()
    }

    fun hide() {
        isVisible = false
        stage?.setScrollFocus(null)
        stage?.setKeyboardFocus(null)
        stage?.cancelTouchFocus()
    }

    fun toggle() {
        isVisible = !isVisible
        if (isVisible) {
            toFront()
        } else {
            stage?.setScrollFocus(null)
            stage?.setKeyboardFocus(null)
            stage?.cancelTouchFocus()
        }
    }
}

