package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.session.GameMode
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.shared.ecs.components.BarracksComponent
import com.mjm.elixir_reign.shared.ecs.components.BarracksTrainingProgress
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.TrainedUnitComponent
import com.mjm.elixir_reign.shared.logic.EntityType

class BarracksProductionSystem(
    private val gameEngine: Engine
) : IteratingSystem(
    Family.all(BarracksComponent::class.java, PositionComponent::class.java).get()
) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (GameSession.mode == GameMode.MULTI) return

        val barracks = entity.getComponent(BarracksComponent::class.java)
        val position = entity.getComponent(PositionComponent::class.java)

        spawnReadyUnits(barracks, position)

        if (barracks.activeTraining == null && barracks.trainingQueue.isNotEmpty() && plannedUnitCount(barracks) <= barracks.maxFormedUnits) {
            barracks.activeTraining = BarracksTrainingProgress(barracks.trainingQueue.removeAt(0))
        }

        val activeTraining = barracks.activeTraining ?: return
        activeTraining.elapsedSeconds += deltaTime

        val stats = SpriteEntityFactory.getUnitStats(activeTraining.unitType)
        if (activeTraining.elapsedSeconds >= stats.trainingTimeSeconds) {
            if (formedUnitCount(barracks) < barracks.maxFormedUnits) {
                spawnUnit(unitType = activeTraining.unitType, barracks = barracks, position = position)
            }
            barracks.activeTraining = null
        }
    }

    private fun spawnReadyUnits(barracks: BarracksComponent, position: PositionComponent) {
        while (barracks.readyToSpawn.isNotEmpty() && formedUnitCount(barracks) < barracks.maxFormedUnits) {
            val unitType = barracks.readyToSpawn.removeAt(0)
            spawnUnit(unitType = unitType, barracks = barracks, position = position)
        }
    }

    private fun spawnUnit(unitType: EntityType, barracks: BarracksComponent, position: PositionComponent) {
        val angle = MathUtils.random(0f, MathUtils.PI2)
        val radius = MathUtils.random(MIN_SPAWN_RADIUS, MAX_SPAWN_RADIUS)
        SpriteEntityFactory.createUnit(
            entityType = unitType,
            x = position.x + MathUtils.cos(angle) * radius,
            y = position.y + MathUtils.sin(angle) * radius,
            engine = gameEngine,
            barracksId = barracks.barracksId,
            teamId = barracks.teamId
        )
    }

    private fun formedUnitCount(barracks: BarracksComponent): Int {
        var count = 0
        for (entity in gameEngine.entities) {
            val trainedUnit = entity.getComponent(TrainedUnitComponent::class.java) ?: continue
            if (trainedUnit.barracksId == barracks.barracksId && trainedUnit.teamId == barracks.teamId) {
                count++
            }
        }
        return count
    }

    private fun plannedUnitCount(barracks: BarracksComponent): Int {
        val activeCount = if (barracks.activeTraining == null) 0 else 1
        return formedUnitCount(barracks) + barracks.readyToSpawn.size + barracks.trainingQueue.size + activeCount
    }

    companion object {
        private const val MIN_SPAWN_RADIUS = 55f
        private const val MAX_SPAWN_RADIUS = 95f
    }
}
