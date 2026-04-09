package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.mjm.elixir_reign.core.i18n.Localization

object Shop : Table() {

    private val itemsTable = Table()
    private val container = Table()
    private lateinit var scrollPane: ScrollPane

    init{
        // Keep root touchable so we can clear focus on transparent clicks.
        touchable = Touchable.enabled

        setFillParent(true)
        align(Align.bottom)
        isVisible = true

        container.background = UiAssets.skin.getDrawable("shopBackground")
        container.pad(20f)

        val title = Label(Localization.get("shop.title"), UiAssets.skin)
        title.setAlignment(Align.left)

        val closeBtn = ImageButton(UiAssets.skin.get("shopCloseButton", ImageButton.ImageButtonStyle::class.java))
        closeBtn.imageCell.pad(5f)
        closeBtn.addListener(object : ClickListener(){
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                hide()
            }
        })

        val topTable = Table()
        topTable.add(title).expandX().fillX().left().padBottom(10f)
        topTable.add(closeBtn).size(30f).padBottom(10f)

        // Horizontal list of shop cards.
        itemsTable.defaults().padRight(12f)
        for(i in 1..20) {
            val itemCard = ShopCard("Item $i", i * 10)
            itemsTable.add(itemCard)
        }

        scrollPane = ScrollPane(itemsTable, UiAssets.skin, "shopTransparent")
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

                // Keep focus when moving between ScrollPane children; clear only when leaving it.
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

                // Map wheel/trackpad vertical movement to horizontal card scrolling.
                val scrollAmount = (amountY + amountX) * 75f
                scrollPane.scrollX += scrollAmount
                scrollPane.updateVisualScroll()
                return true
            }
        })

        container.add(topTable).expandX().fillX().row()
        container.add(scrollPane).expandX().fillX().height(220f)

        add(container).expand().fillX().bottom()

        addCaptureListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (!isVisible) {
                    return false
                }

                val insideContainer = x in container.x..(container.x + container.width) &&
                    y in container.y..(container.y + container.height)

                if (!insideContainer) {
                    // Release stale UI focus so wheel/gestures can go back to world input.
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
