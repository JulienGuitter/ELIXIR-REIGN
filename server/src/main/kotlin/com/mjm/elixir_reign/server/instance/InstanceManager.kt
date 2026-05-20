package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.ConfigManager
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.type.GameType

object InstanceManager {
    private var config = ConfigManager.getConfig()

    private var instances = mutableListOf<Instance>()
    var isInit = false
        private set

    fun init(){
        for(i in 0 until config.maxInstances){
            instances.add(Instance())
        }

        isInit = true
    }

    /**
     * Retourne le nombre d'instances disponibles (non actives)
     */
    fun getAvailableInstances(): Int{
        return instances.count { !it.active }
    }

    /**
     * Crée (active) une instance pour un gameType donné.
     * Retourne l'instance créée ou null si aucune disponible.
     */
    fun createInstance(gameType: GameType): Instance? {
        val instance = instances.firstOrNull { !it.active } ?: return null
        instance.start(gameType)
        return instance
    }

    /**
     * Trouve une instance par son UUID
     */
    fun findByUUID(uuid: String): Instance? {
        return instances.firstOrNull { it.uuid == uuid && it.active }
    }

    fun removePlayer(id: Int) {
        instances
            .filter { it.active && it.containsConnection(id) }
            .forEach { it.removePlayer(id) }
    }

    fun handleMoveRequest(playerId: Int, unitIds: IntArray, targetRow: Int, targetCol: Int) {
        val instance = instances.firstOrNull { it.active && it.containsConnection(playerId) } ?: return
        val resolvedPlayerId = instance.playerIdForConnection(playerId) ?: return
        instance.handleMoveRequest(resolvedPlayerId, unitIds, targetRow, targetCol)
    }

    fun handlePlaceBuildingRequest(connectionId: Int, requestId: Int, entityType: EntityType, row: Int, col: Int) {
        val instance = instances.firstOrNull { it.active && it.containsConnection(connectionId) } ?: return
        val resolvedPlayerId = instance.playerIdForConnection(connectionId) ?: return
        instance.handlePlaceBuildingRequest(connectionId, resolvedPlayerId, requestId, entityType, row, col)
    }

    fun handleUpgradeBuildingRequest(connectionId: Int, requestId: Int, buildingId: Int) {
        val instance = instances.firstOrNull { it.active && it.containsConnection(connectionId) } ?: return
        val resolvedPlayerId = instance.playerIdForConnection(connectionId) ?: return
        instance.handleUpgradeBuildingRequest(connectionId, resolvedPlayerId, requestId, buildingId)
    }

    fun update(deltaSeconds: Float) {
        instances
            .filter { it.active }
            .forEach { it.update(deltaSeconds) }
    }
}
