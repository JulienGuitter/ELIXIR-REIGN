package com.mjm.elixir_reign.core.ui

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.ecs.components.BarracksComponent
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.TrainedUnitComponent
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.EntityType
import kotlin.math.roundToInt

class BarracksPanel(
    private val barracksProvider: () -> List<Entity>,
    private val allEntitiesProvider: () -> Iterable<Entity>,
    private val removeEntity: (Entity) -> Unit,
    private val onBarracksFocused: (Entity) -> Unit,
    private val onTrainUnitRequested: (Entity, EntityType) -> Boolean = { _, _ -> false },
    private val canMutateLocally: () -> Boolean = { true }
) : Table() {
    private val container = Table()
    private val titleLabel = Label("", UiAssets.skin)
    private val availableUnitsTable = Table()
    private val activeTrainingTable = Table().apply { left() }
    private val formedUnitsTable = Table().apply { left() }
    private val statusLabel = Label("", UiAssets.skin)
    private val trainButtons = mutableMapOf<EntityType, TextButton>()
    private val overlayTexture: Texture = UiAssets.createRoundedRectTexture(2, 2, 0, Color.WHITE)
    private val multiplierBackground = TextureRegionDrawable(TextureRegion(overlayTexture))
        .tint(Color(0f, 0f, 0f, 0.48f))

    private var selectedBarracks: Entity? = null

    init {
        setFillParent(true)
        align(Align.bottom)
        isVisible = false
        touchable = Touchable.disabled

        container.background = UiAssets.skin.getDrawable("shopBackground")
        container.pad(18f)
        container.touchable = Touchable.enabled
        container.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return isVisible
            }
        })

        val previousButton = TextButton("<", UiAssets.skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    selectRelativeBarracks(-1)
                }
            })
        }

        val nextButton = TextButton(">", UiAssets.skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    selectRelativeBarracks(1)
                }
            })
        }

        val closeButton = ImageButton(UiAssets.skin.get("shopCloseButton", ImageButton.ImageButtonStyle::class.java)).apply {
            imageCell.pad(8f)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    hide()
                }
            })
        }

        titleLabel.setAlignment(Align.center)
        titleLabel.setFontScale(0.8f)
        statusLabel.setAlignment(Align.left)
        statusLabel.setFontScale(0.62f)

        val topTable = Table()
        topTable.add(previousButton).size(58f, 48f).padRight(8f)
        topTable.add(titleLabel).expandX().fillX()
        topTable.add(nextButton).size(58f, 48f).padLeft(8f).padRight(8f)
        topTable.add(closeButton).size(48f)

        availableUnitsTable.defaults().padRight(10f)
        val availableScrollPane = ScrollPane(availableUnitsTable, UiAssets.skin, "shopTransparent").apply {
            setFadeScrollBars(false)
            setScrollingDisabled(false, true)
            setForceScroll(false, false)
        }

        val statusTable = Table().apply {
            top().left()
            defaults().padBottom(10f)
        }
        statusTable.add(sectionTitle("En formation")).width(190f).left().top().padRight(12f)
        statusTable.add(activeTrainingTable).expandX().fillX().height(70f).left().top().row()

        val formedHeader = Table().apply {
            left().top()
            add(sectionTitle("Formees")).left().row()
            add(statusLabel).left().padTop(4f)
        }
        statusTable.add(formedHeader).width(190f).left().top().padRight(12f)
        statusTable.add(formedUnitsTable).expandX().fillX().height(70f).left().top()

        container.add(topTable).expandX().fillX().row()
        container.add(availableScrollPane).expandX().fillX().height(190f).padTop(8f).row()
        container.add(statusTable).expandX().fillX().height(160f).padTop(8f)

        add(container).expand().fillX().bottom()

        addCaptureListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return false
            }
        })
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (isVisible) {
            refresh()
        }
    }

    fun showFor(barracks: Entity) {
        selectedBarracks = barracks
        isVisible = true
        touchable = Touchable.enabled
        toFront()
        onBarracksFocused(barracks)
        rebuildAvailableUnits()
        refresh()
    }

    fun hide() {
        isVisible = false
        touchable = Touchable.disabled
        selectedBarracks = null
        stage?.setScrollFocus(null)
        stage?.setKeyboardFocus(null)
        stage?.cancelTouchFocus()
    }

    private fun selectRelativeBarracks(offset: Int) {
        val barracks = barracksProvider()
        if (barracks.isEmpty()) {
            hide()
            return
        }

        val current = selectedBarracks
        val currentIndex = barracks.indexOf(current).takeIf { it >= 0 } ?: 0
        val nextIndex = Math.floorMod(currentIndex + offset, barracks.size)
        showFor(barracks[nextIndex])
    }

    private fun rebuildAvailableUnits() {
        availableUnitsTable.clearChildren()
        trainButtons.clear()
        listOf(EntityType.BARBARIAN, EntityType.ARCHER, EntityType.GIANT).forEach { unitType ->
            availableUnitsTable.add(createUnitCard(unitType)).width(260f).height(176f)
        }
    }

    private fun createUnitCard(unitType: EntityType): Actor {
        val stats = SpriteEntityFactory.getUnitStats(unitType)
        val card = Table().apply {
            background = UiAssets.skin.getDrawable("shopCardBackground")
            pad(10f)
        }

        val image = unitImage(unitType)

        val nameLabel = Label(stats.name, UiAssets.skin).apply {
            setFontScale(0.7f)
            setAlignment(Align.left)
        }
        val statsLabel = Label(statsText(stats), UiAssets.skin).apply {
            setFontScale(0.48f)
            setAlignment(Align.left)
            setWrap(true)
        }
        val costLabel = Label(costText(stats), UiAssets.skin).apply {
            setFontScale(0.48f)
            color = Color(1f, 0.88f, 0.52f, 1f)
            setWrap(true)
        }
        val trainButton = TextButton("Former", UiAssets.skin).apply {
            label.setFontScale(0.6f)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val barracks = selectedBarracks?.getComponent(BarracksComponent::class.java) ?: return
                    if (!canQueueUnit(barracks)) {
                        refresh()
                        return
                    }
                    if (canMutateLocally()) {
                        barracks.trainingQueue.add(unitType)
                    } else {
                        onTrainUnitRequested(selectedBarracks ?: return, unitType)
                    }
                    refresh()
                }
            })
        }
        trainButtons[unitType] = trainButton

        val textTable = Table()
        textTable.add(nameLabel).expandX().fillX().left().row()
        textTable.add(statsLabel).expandX().fillX().left().row()
        textTable.add(costLabel).expandX().fillX().left().padTop(2f)

        card.add(image).size(62f).padRight(8f)
        card.add(textTable).expandX().fillX().left().row()
        card.add(trainButton).colspan(2).expandX().fillX().height(40f).padTop(8f)
        return card
    }

    private fun refresh() {
        val barracksEntity = selectedBarracks ?: return
        val barracks = barracksEntity.getComponent(BarracksComponent::class.java) ?: return
        val barracksList = barracksProvider()
        val index = barracksList.indexOf(barracksEntity).takeIf { it >= 0 } ?: 0
        val formedCount = formedUnitCount(barracks)
        val plannedCount = plannedUnitCount(barracks)
        val hasCapacity = canQueueUnit(barracks)

        titleLabel.setText("Caserne ${index + 1}/${barracksList.size} - $plannedCount/${barracks.maxFormedUnits}")
        statusLabel.setText("Troupes formees: $formedCount/${barracks.maxFormedUnits}")
        trainButtons.values.forEach { button ->
            button.isDisabled = !hasCapacity
            button.setText(if (hasCapacity) "Former" else "Plein")
        }

        activeTrainingTable.clearChildren()
        val active = barracks.activeTraining
        if (active == null && barracks.trainingQueue.isEmpty() && barracks.readyToSpawn.isEmpty()) {
            activeTrainingTable.add(smallLabel("Aucune")).left()
        } else {
            if (active != null) {
                val stats = SpriteEntityFactory.getUnitStats(active.unitType)
                val progress = (active.elapsedSeconds / stats.trainingTimeSeconds).coerceIn(0f, 1f)
                activeTrainingTable.add(deletableUnitSlot(
                    unitType = active.unitType,
                    progress = progress,
                    multiplier = null,
                    canDelete = canMutateLocally()
                ) {
                    barracks.activeTraining = null
                    refresh()
                }).left().padRight(12f)
            }
            barracks.trainingQueue.groupingBy { it }.eachCount().forEach { (unitType, count) ->
                activeTrainingTable.add(deletableUnitSlot(
                    unitType = unitType,
                    progress = null,
                    multiplier = count,
                    canDelete = canMutateLocally()
                ) {
                    barracks.trainingQueue.remove(unitType)
                    refresh()
                }).left().padRight(12f)
            }
            barracks.readyToSpawn.groupingBy { it }.eachCount().forEach { (unitType, count) ->
                activeTrainingTable.add(deletableUnitSlot(
                    unitType = unitType,
                    progress = 1f,
                    multiplier = count,
                    canDelete = canMutateLocally()
                ) {
                    barracks.readyToSpawn.remove(unitType)
                    refresh()
                }).left().padRight(12f)
            }
        }

        formedUnitsTable.clearChildren()
        val formedByType = formedUnitEntitiesFor(barracks)
            .mapNotNull { it.getComponent(EntityTypeComponent::class.java)?.entityType }
            .groupingBy { it }
            .eachCount()
        if (formedByType.isEmpty()) {
            formedUnitsTable.add(smallLabel("Aucune")).left()
        } else {
            formedByType.forEach { (unitType, count) ->
                formedUnitsTable.add(deletableUnitSlot(
                    unitType = unitType,
                    progress = null,
                    multiplier = count,
                    canDelete = canMutateLocally()
                ) {
                    formedUnitEntitiesFor(barracks)
                        .firstOrNull { it.getComponent(EntityTypeComponent::class.java)?.entityType == unitType }
                        ?.let(removeEntity)
                    refresh()
                }).left().padRight(12f)
            }
        }
    }

    private fun formedUnitEntitiesFor(barracks: BarracksComponent): List<Entity> {
        val formedUnits = mutableListOf<Entity>()
        for (entity in allEntitiesProvider()) {
            val trainedUnit = entity.getComponent(TrainedUnitComponent::class.java) ?: continue
            if (trainedUnit.barracksId != barracks.barracksId || trainedUnit.teamId != barracks.teamId) {
                continue
            }
            val health = entity.getComponent(HealthComponent::class.java)
            if (health != null && health.currentHP <= 0f) {
                continue
            }
            if (entity.getComponent(EntityTypeComponent::class.java) != null) {
                formedUnits.add(entity)
            }
        }
        return formedUnits
    }

    private fun formedUnitCount(barracks: BarracksComponent): Int = formedUnitEntitiesFor(barracks).size

    private fun plannedUnitCount(barracks: BarracksComponent): Int {
        val activeCount = if (barracks.activeTraining == null) 0 else 1
        return formedUnitCount(barracks) + barracks.readyToSpawn.size + barracks.trainingQueue.size + activeCount
    }

    private fun canQueueUnit(barracks: BarracksComponent): Boolean {
        return plannedUnitCount(barracks) < barracks.maxFormedUnits
    }

    private fun sectionTitle(text: String): Label {
        return Label(text, UiAssets.skin).apply {
            setFontScale(0.62f)
            color = Color(1f, 0.88f, 0.52f, 1f)
        }
    }

    private fun smallLabel(text: String): Label {
        return Label(text, UiAssets.skin).apply {
            setFontScale(0.5f)
            setAlignment(Align.left)
        }
    }

    private fun deletableUnitSlot(
        unitType: EntityType,
        progress: Float?,
        multiplier: Int?,
        canDelete: Boolean,
        onDelete: () -> Unit
    ): Table {
        val table = Table()
        val slot = Stack()

        val background = Table().apply {
            background = UiAssets.skin.getDrawable("shopCardBackground")
        }
        val icon = Table().apply {
            add(unitImage(unitType)).size(50f)
        }
        slot.add(background)
        slot.add(icon)

        progress?.let { value ->
            slot.add(ProgressOverlay(overlayTexture, value))
        }

        if (multiplier != null && multiplier > 1) {
            slot.add(multiplierBadge(multiplier))
        }

        table.add(slot).size(66f).padRight(5f)
        if (canDelete) {
            table.add(deleteButton(onDelete)).size(32f, 30f).top()
        }
        return table
    }

    private fun deleteButton(onDelete: () -> Unit): TextButton {
        return TextButton("X", UiAssets.skin).apply {
            label.setFontScale(0.45f)
            addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    onDelete()
                    return true
                }
            })
        }
    }

    private fun multiplierBadge(multiplier: Int): Table {
        val badge = Table().apply {
            background = multiplierBackground
        }
        val label = Label("x$multiplier", UiAssets.skin).apply {
            setFontScale(0.42f)
            color = Color(1f, 1f, 1f, 0.78f)
        }
        badge.add(label).pad(1f, 5f, 1f, 5f)

        return Table().apply {
            bottom().right()
            add(badge).padRight(3f).padBottom(3f)
        }
    }

    private fun unitImage(unitType: EntityType): Image {
        return Image(TextureRegionDrawable(unitTexture(unitType))).apply {
            setScaling(Scaling.fit)
        }
    }

    private fun statsText(stats: UnitStats): String {
        return "PV ${stats.maxHP.roundToInt()}  ATQ ${stats.damage.roundToInt()}  VIT ${stats.speed.roundToInt()}  POR ${stats.range.roundToInt()}"
    }

    private fun costText(stats: UnitStats): String {
        val costs = mutableListOf<String>()
        if (stats.costGold > 0) costs += "${stats.costGold} or"
        if (stats.costElixir > 0) costs += "${stats.costElixir} elixir"
        if (stats.costDarkElixir > 0) costs += "${stats.costDarkElixir} elixir noir"
        val costText = if (costs.isEmpty()) "aucun cout" else costs.joinToString(", ")
        return "$costText  ${stats.trainingTimeSeconds.roundToInt()}s"
    }

    private fun unitTexture(unitType: EntityType): TextureRegion {
        return SpriteAnimationManager.createUnitAnimator(
            stats = SpriteEntityFactory.getUnitStats(unitType),
            actionType = ActionType.RUN,
            directionType = DirectionType.DOWN
        ).getCurrentTextureRegion() ?: TextureRegion()
    }

    private class ProgressOverlay(
        private val texture: Texture,
        private val progress: Float
    ) : Actor() {
        override fun draw(batch: Batch, parentAlpha: Float) {
            val previousColor = batch.color.cpy()
            batch.setColor(1f, 1f, 1f, 0.34f * parentAlpha)
            batch.draw(texture, x, y, width, height * progress.coerceIn(0f, 1f))
            batch.color = previousColor
        }
    }
}
