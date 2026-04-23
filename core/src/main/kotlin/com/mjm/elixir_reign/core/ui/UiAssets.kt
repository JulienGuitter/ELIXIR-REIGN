package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.ScreenUtils
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.shared.GameConfiguration
import java.util.EnumMap

object UiAssets {

    var screenResolution = com.badlogic.gdx.math.Vector2(1920f, 1080f)
        private set

    private const val BUTTON_PATCH_INSET = 20

    lateinit var skin: Skin
        private set
    private lateinit var buttonNinePatch: NinePatch
    private lateinit var leftPanelNinePatch: NinePatch

    private val textures = EnumMap<UiImage, Texture>(UiImage::class.java)

    private const val FONT_ASSET_NAME = "fonts/Macondo-Regular.ttf"

    fun texture(image: UiImage): Texture = textures.getValue(image)

    /*
     * Applique le filtre de texture approprié (linéaire ou nearest) en fonction de la configuration de l'image.
     */
    private fun applyFilter(tex: Texture, linear: Boolean){
        val filter = if(linear) Texture.TextureFilter.Linear else Texture.TextureFilter.Nearest
        tex.setFilter(filter, filter)
    }

    /**
     * Charge les assets marqués "minimal" directement, pour pouvoir afficher une UI basique pendant le chargement async des autres assets via AssetManager.
     */
    fun loadMinimal() {

        UiImage.entries
            .filter { it.minimal }
            .forEach { img ->
                val tex = Texture(img.path)
                applyFilter(tex, img.linearFilter)
                textures[img] = tex
            }
    }

    /**
     * Enregistre tous les assets UI + sprites dans l'AssetManager pour le chargement async.
     * Appelé depuis LoadingScreen.show().
     */
    fun queueLoading(assets: AssetManager) {
        // Enregistrer les loaders FreeType TOUJOURS avant tout load()
        // setLoader écrase le loader existant — c'est intentionnel pour .ttf
        val resolver: FileHandleResolver = InternalFileHandleResolver()
        assets.setLoader(FreeTypeFontGenerator::class.java, FreeTypeFontGeneratorLoader(resolver))
        assets.setLoader(BitmapFont::class.java, ".ttf", FreetypeFontLoader(resolver))

        // Font via AssetManager — le nom doit se terminer par .ttf pour matcher le loader
        val fontParams = FreetypeFontLoader.FreeTypeFontLoaderParameter().apply {
            fontFileName = "fonts/Macondo-Regular.ttf"
            fontParameters.size = 32
            fontParameters.color = Color.WHITE
            fontParameters.borderWidth = 0.5f
            fontParameters.borderColor = Color(0f, 0f, 0f, 0.3f)
        }
        assets.load(FONT_ASSET_NAME, BitmapFont::class.java, fontParams)

        UiImage.entries
            .filter { it.loadedByAssetManager }
            .forEach { img -> assets.load(img.path, Texture::class.java) }

        // Sprites du jeu
        TextureManager.queueAll(assets)
    }

    /**
     * Récupère les assets du AssetManager une fois le chargement terminé, et crée les styles de skin.
     */
    fun finishLoading(assets: AssetManager) {

        UiImage.entries
            .filter { it.loadedByAssetManager }
            .forEach { img ->
                if (!assets.isLoaded(img.path, Texture::class.java)) {
                    Gdx.app.error("UiAssets", "Asset not loaded: ${img.path}, keeping existing texture")
                    return@forEach
                }
                // si déjà chargé en minimal et remplacé ensuite, on dispose avant overwrite
                textures[img]?.takeIf { it !== assets.get(img.path, Texture::class.java) }?.dispose()
                val tex = assets.get(img.path, Texture::class.java)
                applyFilter(tex, img.linearFilter)
                textures[img] = tex
            }

        val font: BitmapFont = if (assets.isLoaded(FONT_ASSET_NAME, BitmapFont::class.java)) {
            assets.get(FONT_ASSET_NAME, BitmapFont::class.java).also { it.color = Color.WHITE }
        } else {
            Gdx.app.error("UiAssets", "Font not loaded, using fallback")
            BitmapFont().apply { data.setScale(2f); color = Color.WHITE; data.markupEnabled = true }
        }

        skin = Skin()
        skin.add("default", font, BitmapFont::class.java)

        buttonNinePatch = NinePatch(
            texture(UiImage.BUTTON_9PATCH),
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET
        )

        leftPanelNinePatch = NinePatch(
            texture(UiImage.LEFT_PANEL_9PATCH),
            0,
            0,
            BUTTON_PATCH_INSET,
            0
        )

        val buttonUp = makeButtonDrawable(buttonNinePatch, Color(0.9f, 0.9f, 0.9f, 1f))
        val buttonDown = makeButtonDrawable(buttonNinePatch, Color(0.6f, 0.6f, 0.6f, 1f))
        val buttonOver = makeButtonDrawable(buttonNinePatch, Color.WHITE)

        registerBaseWidgetStyles(font, buttonUp, buttonDown, buttonOver)
        registerShopStyles(assets, leftPanelNinePatch)

        // Default scroll pane style
        val defaultScrollStyle = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = buttonUp
        }
        skin.add("default", defaultScrollStyle, com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle::class.java)

    }

    private fun registerBaseWidgetStyles(
        font: BitmapFont,
        buttonUp: Drawable,
        buttonDown: Drawable,
        buttonOver: Drawable
    ) {

        skin.add("default", TextButton.TextButtonStyle().apply {
            up = buttonUp; down = buttonDown; over = buttonOver
            this.font = font; fontColor = Color.WHITE
        }, TextButton.TextButtonStyle::class.java)

        skin.add("default", Label.LabelStyle().apply {
            this.font = font; fontColor = Color.WHITE
        }, Label.LabelStyle::class.java)

        val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle().apply {
            this.font = font
            fontColorUnselected = Color.WHITE
            fontColorSelected = Color.WHITE
            selection = buttonDown
        }

        val selectBoxDropdownScrollStyle = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = buttonUp
        }
        skin.add("default", SelectBox.SelectBoxStyle().apply {
            this.font = font
            background = buttonUp
            this.listStyle = listStyle
            this.scrollStyle = selectBoxDropdownScrollStyle
        }, SelectBox.SelectBoxStyle::class.java)
    }

    private fun registerShopStyles(
        assets: AssetManager,
        leftPanelTexture: NinePatch
    ) {

        val shopBg = NinePatchDrawable(leftPanelTexture).apply {
            tint(Color(0.8f, 0.8f, 0.8f, 1f))
            setMinWidth(200f)
            setMinHeight(200f)
        }
        skin.add("shopBackground", shopBg, Drawable::class.java)

        val shopCardTexture = assets.get("ui/shop_card_9patch.png", Texture::class.java).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        val shopCardPatch = NinePatch(
            shopCardTexture,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET
        )
        val shopCardBg = NinePatchDrawable(shopCardPatch).apply {
            setMinWidth(200f)
            setMinHeight(80f)
        }
        skin.add("shopCardBackground", shopCardBg, Drawable::class.java)

        val transparentShopScrollStyle = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {
            background = null
        }
        skin.add("shopTransparent", transparentShopScrollStyle, com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle::class.java)

        val closeIconTexture = assets.get("icons/close.png", Texture::class.java).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        val closeButtonBgTexture = createRoundedRectTexture(
            width = 32,
            height = 32,
            radius = 8,
            color = Color(0.85f, 0.15f, 0.15f, 1f)
        )
        skin.add("shopCloseButtonBgTexture", closeButtonBgTexture, Texture::class.java)
        skin.add("shopCloseButton", createShopCloseButtonStyle(closeButtonBgTexture, closeIconTexture), ImageButton.ImageButtonStyle::class.java)
    }

    private fun createShopCloseButtonStyle(
        closeButtonBgTexture: Texture,
        closeIconTexture: Texture
    ): ImageButton.ImageButtonStyle {
        return ImageButton.ImageButtonStyle().apply {
            up = TextureRegionDrawable(TextureRegion(closeButtonBgTexture))
            over = TextureRegionDrawable(TextureRegion(closeButtonBgTexture)).tint(Color(0.95f, 0.25f, 0.25f, 1f))
            down = TextureRegionDrawable(TextureRegion(closeButtonBgTexture)).tint(Color(0.7f, 0.1f, 0.1f, 1f))
            imageUp = TextureRegionDrawable(TextureRegion(closeIconTexture))
            imageOver = TextureRegionDrawable(TextureRegion(closeIconTexture))
            imageDown = TextureRegionDrawable(TextureRegion(closeIconTexture)).tint(Color(1f, 1f, 1f, 0.9f))
        }

    }

    fun dispose() {
        if (::skin.isInitialized) skin.dispose()

        UiImage.entries
            .filter { !it.loadedByAssetManager } // chargées "à la main"
            .forEach { img -> textures[img]?.dispose() }

        textures.clear()
    }

    fun getProgress(assets: AssetManager): Float = assets.progress

    private fun makeButtonDrawable(basePatch: NinePatch, tint: Color): Drawable {
        return NinePatchDrawable(basePatch)
            .tint(tint).apply {
                setMinWidth(100f)
                setMinHeight(50f)
            }
    }

    fun drawBackground(stage: Stage, spriteBatch: SpriteBatch) {
        ScreenUtils.clear(Color.BLACK)
        stage.viewport.apply()

        val bg = texture(UiImage.BACKGROUND)

        val worldWidth = stage.viewport.worldWidth
        val worldHeight = stage.viewport.worldHeight
        val texWidth = bg.width.toFloat()
        val texHeight = bg.height.toFloat()
        val scale = maxOf(worldWidth / texWidth, worldHeight / texHeight)
        val drawWidth = texWidth * scale
        val drawHeight = texHeight * scale
        val xBack = (worldWidth - drawWidth) / 2f
        val yBack = (worldHeight - drawHeight) / 2f

        spriteBatch.projectionMatrix.set(stage.viewport.camera.combined)
        spriteBatch.begin()
        spriteBatch.draw(bg, xBack, yBack, drawWidth, drawHeight)
        spriteBatch.end()
    }

    fun createVersionTable(): Table {
        // Label version en bas à droite
        val versionLabel = Label("v${GameConfiguration.VERSION}", UiAssets.skin).apply {
            color = Color(1f, 1f, 1f, 0.6f)
        }
        return Table().apply {
            setFillParent(true)
            bottom().right()
            add(versionLabel).pad(12f)
        }
    }

    fun createRoundedRectTexture(
        width: Int,
        height: Int,
        radius: Int,
        color: Color
    ): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.SourceOver)

        fun fillRoundedRect(x: Int, y: Int, w: Int, h: Int, r: Int) {
            pixmap.fillRectangle(x + r, y, w - 2 * r, h)
            pixmap.fillRectangle(x, y + r, w, h - 2 * r)

            pixmap.fillCircle(x + r, y + r, r)
            pixmap.fillCircle(x + w - r - 1, y + r, r)
            pixmap.fillCircle(x + r, y + h - r - 1, r)
            pixmap.fillCircle(x + w - r - 1, y + h - r - 1, r)
        }

        pixmap.setColor(color)
        fillRoundedRect(0, 0, width, height, radius)

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }
}
