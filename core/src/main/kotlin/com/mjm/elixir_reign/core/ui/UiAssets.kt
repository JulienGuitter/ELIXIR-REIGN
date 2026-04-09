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

object UiAssets {
    private const val BUTTON_PATCH_INSET = 20

    lateinit var skin: Skin
        private set

    lateinit var backgroundTexture: Texture
        private set
    lateinit var logoTransparent: Texture
        private set
    lateinit var buttonTexture: Texture
        private set
    lateinit var leftPanelTexture: Texture
        private set
    private lateinit var buttonNinePatch: NinePatch
    private lateinit var leftPanelNinePatch: NinePatch
    lateinit var iconHammer: Texture
        private set
    lateinit var iconSelect: Texture
        private set

    private const val FONT_ASSET_NAME = "fonts/Macondo-Regular.ttf"

    /**
     * Charge UNIQUEMENT le logo — appelé dans Main.create() avant le LoadingScreen.
     * Doit rester minimal pour ne pas bloquer le démarrage.
     */
    fun loadMinimal() {
        logoTransparent = Texture("ui/icon_transp.png").also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        backgroundTexture = Texture("ui/background.png").also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        iconHammer = Texture("icons/hammer.png").also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        iconSelect = Texture("icons/selection.png").also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
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

        // Textures UI (background déjà chargé dans loadMinimal)
        assets.load("ui/btn_9patch.png", Texture::class.java)
        assets.load("ui/left_panel_9patch.png", Texture::class.java)
        assets.load("ui/shop_card_9patch.png", Texture::class.java)
        assets.load("ui/icon_transp.png", Texture::class.java)
        assets.load("icons/close.png", Texture::class.java)

        // Sprites du jeu
        TextureManager.queueAll(assets)
    }

    /**
     * Construit le Skin à partir des assets chargés par l'AssetManager.
     * Appelé depuis Main.onAssetsLoaded(), après que assets.update() retourne true.
     */
    fun finishLoading(assets: AssetManager) {
        // background déjà chargé dans loadMinimal(), on ne le re-fetch pas
        buttonTexture = assets.get("ui/btn_9patch.png", Texture::class.java).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        leftPanelTexture = assets.get("ui/left_panel_9patch.png", Texture::class.java).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        // Remplacer le logo minimal par la version chargée par l'AssetManager
        if(this::logoTransparent.isInitialized){
            logoTransparent.dispose()
        }
        logoTransparent = assets.get("ui/icon_transp.png", Texture::class.java).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
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
            buttonTexture,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET,
            BUTTON_PATCH_INSET
        )

        leftPanelNinePatch = NinePatch(
            leftPanelTexture,
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
            fontColorUnselected = Color.WHITE; fontColorSelected = Color.BLACK
            selection = buttonDown
        }
        val scrollPaneStyle = com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle().apply {

        }
        skin.add("default", SelectBox.SelectBoxStyle().apply {
            this.font = font; background = buttonUp
            this.listStyle = listStyle; this.scrollStyle = scrollPaneStyle
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
        // backgroundTexture, buttonTexture, logoTransparent sont gérés par l'AssetManager
        if (::backgroundTexture.isInitialized) backgroundTexture.dispose()
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

        val worldWidth = stage.viewport.worldWidth
        val worldHeight = stage.viewport.worldHeight
        val texWidth = backgroundTexture.width.toFloat()
        val texHeight = backgroundTexture.height.toFloat()
        val scale = maxOf(worldWidth / texWidth, worldHeight / texHeight)
        val drawWidth = texWidth * scale
        val drawHeight = texHeight * scale
        val xBack = (worldWidth - drawWidth) / 2f
        val yBack = (worldHeight - drawHeight) / 2f

        spriteBatch.projectionMatrix.set(stage.viewport.camera.combined)
        spriteBatch.begin()
        spriteBatch.draw(backgroundTexture, xBack, yBack, drawWidth, drawHeight)
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
