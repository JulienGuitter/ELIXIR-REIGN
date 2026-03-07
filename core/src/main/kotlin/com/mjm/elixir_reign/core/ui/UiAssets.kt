package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable

object UiAssets {
    lateinit var skin: Skin
        private set

    lateinit var backgroundTexture: Texture
        private set

    fun load() {
        backgroundTexture = Texture("ui/background.png")
        skin = Skin()

        val font: BitmapFont = try {
            val fontFile = Gdx.files.internal("fonts/Macondo-Regular.ttf")

            if (fontFile.exists()) {
                val generator = FreeTypeFontGenerator(fontFile)
                val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                    size = 32
                    color = Color.WHITE
                    borderWidth = 0.5f
                    borderColor = Color(0f, 0f, 0f, 0.3f)
                }
                val generatedFont = generator.generateFont(parameter)
                generator.dispose()
                generatedFont.color = Color.WHITE
                Gdx.app.log("UiAssets", "Font loaded successfully from: ${fontFile.path()}")
                generatedFont
            } else {
                throw Exception("No TTF font found")
            }
        } catch (_: Exception) {
            BitmapFont().apply {
                data.setScale(2f)
                color = Color.WHITE
                data.markupEnabled = true
            }
        }

        skin.add("default", font, BitmapFont::class.java)

        // Charger la texture 9-patch pour les boutons
        val buttonTexture = Texture("ui/btn_9patch.png")

        // Créer des NinePatch séparés pour chaque état
        // Les marges définissent les zones qui NE SERONT PAS étirées (coins et bordures)
        // Format : left, right, top, bottom (en pixels)
        // Ajustez ces valeurs selon la taille des coins/bordures de votre image
        val patchLeft = 20
        val patchRight = 20
        val patchTop = 20
        val patchBottom = 20

        val buttonUp = NinePatchDrawable(NinePatch(buttonTexture, patchLeft, patchRight, patchTop, patchBottom)).apply {
            tint(Color(0.9f, 0.9f, 0.9f, 1f))
            setMinWidth(100f)
            setMinHeight(50f)
        }
        val buttonDown = NinePatchDrawable(NinePatch(buttonTexture, patchLeft, patchRight, patchTop, patchBottom)).apply {
            tint(Color(0.6f, 0.6f, 0.6f, 1f))
            setMinWidth(100f)
            setMinHeight(50f)
        }
        val buttonOver = NinePatchDrawable(NinePatch(buttonTexture, patchLeft, patchRight, patchTop, patchBottom)).apply {
            tint(Color(1f, 1f, 1f, 1f))
            setMinWidth(100f)
            setMinHeight(50f)
        }

        // Créer le style de bouton
        val textButtonStyle = TextButton.TextButtonStyle().apply {
            up = buttonUp
            down = buttonDown
            over = buttonOver
            this.font = skin.getFont("default")
            fontColor = Color.WHITE
        }

        skin.add("default", textButtonStyle, TextButton.TextButtonStyle::class.java)

        // Créer le style pour les labels (texte blanc)
        val labelStyle = Label.LabelStyle().apply {
            this.font = skin.getFont("default")
            fontColor = Color.WHITE
        }
        skin.add("default", labelStyle, Label.LabelStyle::class.java)

        // Créer ListStyle pour SelectBox
        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle().apply {
            this.font = skin.getFont("default")
            fontColorUnselected = Color.WHITE
            fontColorSelected = Color.BLACK
            selection = buttonDown // Utiliser le drawable du bouton pour la sélection
        }

        // Créer ScrollPaneStyle pour la dropdown
        val scrollPaneStyle = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = buttonUp // Fond du scroll pane
        }

        // Créer le style pour la SelectBox avec ListStyle et ScrollPaneStyle complets
        val selectBoxStyle = SelectBox.SelectBoxStyle().apply {
            this.font = skin.getFont("default")
            background = buttonUp
            this.listStyle = listStyle
            this.scrollStyle = scrollPaneStyle
        }
        skin.add("default", selectBoxStyle, SelectBox.SelectBoxStyle::class.java)
    }

    fun dispose() {
        if (::skin.isInitialized) {
            skin.dispose()
        }
        if (::backgroundTexture.isInitialized) {
            backgroundTexture.dispose()
        }
    }
}












